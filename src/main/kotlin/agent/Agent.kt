package agent

import jdk.internal.org.objectweb.asm.*
import jdk.internal.org.objectweb.asm.Opcodes.*
import java.lang.instrument.Instrumentation

class Agent {
    companion object {
        @JvmStatic
        fun premain(agentArgs: String?, inst: Instrumentation) {
            println("Agent started.")
            inst.addTransformer ({ _, _, _, _, classfileBuffer ->
                val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
                val cv = BeforeInvokeTestClassVisitor(cw)
                val reader = ClassReader(classfileBuffer)
                reader.accept(cv, 0)
                cw.toByteArray()
            }, true)
        }
    }

    private class BeforeInvokeTestClassVisitor internal constructor(classVisitor : ClassVisitor)
        : ClassVisitor(ASM5, classVisitor){
        override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<String>?): MethodVisitor {
            val outMV = super.visitMethod(access, name, desc, signature, exceptions)
            return BeforeInvokeStaticMethodTransformer(outMV)
        }
    }
    private class BeforeInvokeStaticMethodTransformer internal constructor(methodVisitor: MethodVisitor)
        : MethodVisitor(ASM5, methodVisitor){
        private val SYSTEM_CLASS = "java/lang/System"
        private val OUT_CLASS = "java/io/PrintStream"
        override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, desc: String?, itf: Boolean) {
            if (opcode == INVOKESTATIC
                    && owner == "example/CoroutineExampleKt"
                    && name == "test"
                    && desc == "(Lkotlin/coroutines/experimental/Continuation;)Ljava/lang/Object;"){

                //Invoke System.out.println("Test detected")
                mv.visitFieldInsn(GETSTATIC, SYSTEM_CLASS, "out", "L$OUT_CLASS;")
                mv.visitLdcInsn("Test detected")
                mv.visitMethodInsn(INVOKEVIRTUAL, OUT_CLASS, "println", "(Ljava/lang/String;)V", false)
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf)
        }
    }
}

