@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package com.jetbrains.rider.azure.model

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.impl.*

import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.*
import com.jetbrains.rd.util.*
import kotlin.time.Duration
import kotlin.reflect.KClass
import kotlin.jvm.JvmStatic



/**
 * #### Generated from [FunctionAppDaemonModel.kt:14]
 */
class FunctionAppDaemonModel private constructor(
    private val _runFunctionApp: RdSignal<FunctionAppRequest>,
    private val _debugFunctionApp: RdSignal<FunctionAppRequest>,
    private val _triggerFunctionApp: RdSignal<FunctionAppRequest>,
    private val _getAzureFunctionsVersion: RdCall<AzureFunctionsVersionRequest, String?>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            serializers.register(FunctionAppRequest)
            serializers.register(AzureFunctionsVersionRequest)
        }
        
        
        
        
        private val __StringNullableSerializer = FrameworkMarshallers.String.nullable()
        
        const val serializationHash = 8720132529663507412L
        
    }
    override val serializersOwner: ISerializersOwner get() = FunctionAppDaemonModel
    override val serializationHash: Long get() = FunctionAppDaemonModel.serializationHash
    
    //fields
    
    /**
     * Signal from backend to run a Function App locally.
     */
    val runFunctionApp: ISource<FunctionAppRequest> get() = _runFunctionApp
    
    /**
     * Signal from backend to debug a Function App locally.
     */
    val debugFunctionApp: ISource<FunctionAppRequest> get() = _debugFunctionApp
    
    /**
     * Signal from backend to trigger a Function App.
     */
    val triggerFunctionApp: ISource<FunctionAppRequest> get() = _triggerFunctionApp
    
    /**
     * Request from frontend to read the AzureFunctionsVersion MSBuild property.
     */
    val getAzureFunctionsVersion: IRdCall<AzureFunctionsVersionRequest, String?> get() = _getAzureFunctionsVersion
    //methods
    //initializer
    init {
        bindableChildren.add("runFunctionApp" to _runFunctionApp)
        bindableChildren.add("debugFunctionApp" to _debugFunctionApp)
        bindableChildren.add("triggerFunctionApp" to _triggerFunctionApp)
        bindableChildren.add("getAzureFunctionsVersion" to _getAzureFunctionsVersion)
    }
    
    //secondary constructor
    internal constructor(
    ) : this(
        RdSignal<FunctionAppRequest>(FunctionAppRequest),
        RdSignal<FunctionAppRequest>(FunctionAppRequest),
        RdSignal<FunctionAppRequest>(FunctionAppRequest),
        RdCall<AzureFunctionsVersionRequest, String?>(AzureFunctionsVersionRequest, __StringNullableSerializer)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("FunctionAppDaemonModel (")
        printer.indent {
            print("runFunctionApp = "); _runFunctionApp.print(printer); println()
            print("debugFunctionApp = "); _debugFunctionApp.print(printer); println()
            print("triggerFunctionApp = "); _triggerFunctionApp.print(printer); println()
            print("getAzureFunctionsVersion = "); _getAzureFunctionsVersion.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): FunctionAppDaemonModel   {
        return FunctionAppDaemonModel(
            _runFunctionApp.deepClonePolymorphic(),
            _debugFunctionApp.deepClonePolymorphic(),
            _triggerFunctionApp.deepClonePolymorphic(),
            _getAzureFunctionsVersion.deepClonePolymorphic()
        )
    }
    //contexts
    //threading
    override val extThreading: ExtThreadingKind get() = ExtThreadingKind.Default
}
val com.jetbrains.rd.ide.model.Solution.functionAppDaemonModel get() = getOrCreateExtension("functionAppDaemonModel", ::FunctionAppDaemonModel)



/**
 * #### Generated from [FunctionAppDaemonModel.kt:21]
 */
data class AzureFunctionsVersionRequest (
    val projectFilePath: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<AzureFunctionsVersionRequest> {
        override val _type: KClass<AzureFunctionsVersionRequest> = AzureFunctionsVersionRequest::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): AzureFunctionsVersionRequest  {
            val projectFilePath = buffer.readString()
            return AzureFunctionsVersionRequest(projectFilePath)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: AzureFunctionsVersionRequest)  {
            buffer.writeString(value.projectFilePath)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as AzureFunctionsVersionRequest
        
        if (projectFilePath != other.projectFilePath) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + projectFilePath.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("AzureFunctionsVersionRequest (")
        printer.indent {
            print("projectFilePath = "); projectFilePath.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [FunctionAppDaemonModel.kt:15]
 */
data class FunctionAppRequest (
    val methodName: String?,
    val functionName: String?,
    val projectFilePath: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<FunctionAppRequest> {
        override val _type: KClass<FunctionAppRequest> = FunctionAppRequest::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): FunctionAppRequest  {
            val methodName = buffer.readNullable { buffer.readString() }
            val functionName = buffer.readNullable { buffer.readString() }
            val projectFilePath = buffer.readString()
            return FunctionAppRequest(methodName, functionName, projectFilePath)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: FunctionAppRequest)  {
            buffer.writeNullable(value.methodName) { buffer.writeString(it) }
            buffer.writeNullable(value.functionName) { buffer.writeString(it) }
            buffer.writeString(value.projectFilePath)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as FunctionAppRequest
        
        if (methodName != other.methodName) return false
        if (functionName != other.functionName) return false
        if (projectFilePath != other.projectFilePath) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + if (methodName != null) methodName.hashCode() else 0
        __r = __r*31 + if (functionName != null) functionName.hashCode() else 0
        __r = __r*31 + projectFilePath.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("FunctionAppRequest (")
        printer.indent {
            print("methodName = "); methodName.print(printer); println()
            print("functionName = "); functionName.print(printer); println()
            print("projectFilePath = "); projectFilePath.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}
