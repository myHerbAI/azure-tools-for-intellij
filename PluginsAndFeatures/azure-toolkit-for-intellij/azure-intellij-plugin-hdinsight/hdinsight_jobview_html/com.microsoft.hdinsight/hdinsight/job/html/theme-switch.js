"use strict";

function setTheme(mode) {
    let themeMode = mode.toLowerCase()
    const event = new CustomEvent('theme-mode', { detail: themeMode });

    // Remove other theme-mode classes
    let allDocClasses = document.documentElement.classList.values();
    let cls = allDocClasses.next();
    while (!cls.done) {
        if (cls.value.endsWith('-theme-mode')) {
            document.documentElement.classList.remove(cls.value);
        }

        cls = allDocClasses.next();
    }

    document.documentElement.classList.add(themeMode + '-theme-mode');
    document.dispatchEvent(event);
}

document.addEventListener("DOMContentLoaded", function () {
    // Query OS Prefers
    const isOsPreferDark = window.matchMedia("(prefers-color-scheme: dark)");
    const isOsPreferLight = window.matchMedia("(prefers-color-scheme: light)");

    // Listen for changes in the OS settings
    isOsPreferDark.addEventListener("change", function (ev) {
        if (ev.matches) {
            setTheme("dark")
        }
    });

    isOsPreferLight.addEventListener("change", function (ev) {
        if (ev.matches) {
            setTheme("light")
        }
    });

    if (isOsPreferDark.matches) {
        setTheme("dark")
    } else if (isOsPreferLight) {
        setTheme("light")
    }
});