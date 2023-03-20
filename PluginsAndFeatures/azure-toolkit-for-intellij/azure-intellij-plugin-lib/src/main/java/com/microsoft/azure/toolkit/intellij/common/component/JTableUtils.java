/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.component;

import com.intellij.profile.codeInspection.ui.table.ThreeStateCheckBoxRenderer;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class JTableUtils {
    public static void enableBatchSelection(JTable table, int checkboxColumnIndex) {
        final Class<?> columnClass = table.getModel().getColumnClass(checkboxColumnIndex);
        if (columnClass != Boolean.class && columnClass != boolean.class) {
            throw new IllegalArgumentException("value type of checkbox column must be Boolean/boolean!");
        }
        final CheckboxHeaderMouseListener listener = new CheckboxHeaderMouseListener(table, checkboxColumnIndex);
        table.getTableHeader().addMouseListener(listener);
        final TableColumn column = table.getColumnModel().getColumn(checkboxColumnIndex);
        column.setHeaderRenderer(new ThreeStateCheckBoxRenderer());
        column.setHeaderValue(false);
    }

    private static class CheckboxHeaderMouseListener extends MouseAdapter implements TableModelListener {
        private final JTable table;
        private final int colIndex;
        private final TailingDebouncer updateHeaderValue;
        private boolean updatingHeaderValue = false;

        public CheckboxHeaderMouseListener(final JTable table, final int columnIndex) {
            super();
            this.table = table;
            this.colIndex = columnIndex;
            this.table.getModel().addTableModelListener(this);
            this.updateHeaderValue = new TailingDebouncer(this::updateHeaderValueInner, 300);
        }

        /**
         * table header clicked
         */
        @Override
        public void mouseClicked(MouseEvent e) {
            final int columnIndex = this.table.columnAtPoint(e.getPoint());
            if (columnIndex == this.colIndex) {
                final TableColumn checkboxColumn = this.table.getColumnModel().getColumn(this.colIndex);
                final Object value = checkboxColumn.getHeaderValue();
                final boolean checked = Objects.nonNull(value) && (boolean) value;
                checkboxColumn.setHeaderValue(!checked);
                this.table.getTableHeader().repaint();
                this.updatingHeaderValue = true;
                final int rowCount = table.getModel().getRowCount();
                for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                    table.getModel().setValueAt(!checked, rowIndex, this.colIndex);
                }
                this.updatingHeaderValue = false;
            }
        }

        /**
         * table model changed
         */
        @Override
        public void tableChanged(final TableModelEvent e) {
            if ((e.getColumn() == TableModelEvent.ALL_COLUMNS || e.getColumn() == this.colIndex)
                && !this.updatingHeaderValue) {
                this.updateHeaderValue.debounce();
            }
        }

        private void updateHeaderValueInner() {
            int numSelected = 0;
            final TableModel model = this.table.getModel();
            for (int row = 0; row < model.getRowCount(); row++) {
                if ((Boolean) model.getValueAt(row, this.colIndex)) {
                    numSelected++;
                }
            }
            final TableColumn column = table.getColumnModel().getColumn(this.colIndex);
            if (numSelected == model.getRowCount()) {
                column.setHeaderValue(true);
            } else if (numSelected == 0) {
                column.setHeaderValue(false);
            } else {
                column.setHeaderValue(null);
            }
            this.table.getTableHeader().repaint();
        }
    }
}
