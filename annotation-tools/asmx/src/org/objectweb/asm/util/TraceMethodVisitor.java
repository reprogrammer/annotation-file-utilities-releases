/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2005 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.objectweb.asm.util;

import java.util.HashMap;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.TypeAnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.util.attrs.Traceable;

/**
 * A {@link MethodVisitor} that prints a disassembled view of the methods it
 * visits.
 * 
 * @author Eric Bruneton
 */
public class TraceMethodVisitor extends TraceAbstractVisitor implements
        MethodVisitor
{
    /**
     * The {@link MethodVisitor} to which this visitor delegates calls. May be
     * <tt>null</tt>.
     */
    protected MethodVisitor mv;

    /**
     * Tab for bytecode instructions.
     */
    protected String tab2 = "    ";

    /**
     * Tab for table and lookup switch instructions.
     */
    protected String tab3 = "      ";

    /**
     * Tab for labels.
     */
    protected String ltab = "   ";

    /**
     * The label names. This map associate String values to Label keys.
     */
    protected final HashMap labelNames;

    /**
     * Constructs a new {@link TraceMethodVisitor}.
     */
    public TraceMethodVisitor() {
        this(null);
    }

    /**
     * Constructs a new {@link TraceMethodVisitor}.
     * 
     * @param mv the {@link MethodVisitor} to which this visitor delegates
     *        calls. May be <tt>null</tt>.
     */
    public TraceMethodVisitor(final MethodVisitor mv) {
        this.labelNames = new HashMap();
        this.mv = mv;
    }

    // ------------------------------------------------------------------------
    // Implementation of the MethodVisitor interface
    // ------------------------------------------------------------------------

    public AnnotationVisitor visitAnnotation(
        final String desc,
        final boolean visible)
    {
        AnnotationVisitor av = super.visitAnnotation(desc, visible);
        if (mv != null) {
            ((TraceAnnotationVisitor) av).av = mv.visitAnnotation(desc, visible);
        }
        return av;
    }

    // jaime
    public TypeAnnotationVisitor visitTypeAnnotation(
        final String desc, 
        final boolean visible,
        final boolean inCode)
    {
        TypeAnnotationVisitor xav = super.visitTypeAnnotation(desc, visible);
        if (mv != null) {
            ((TraceTypeAnnotationVisitor) xav).xav =
                mv.visitTypeAnnotation(desc, visible, inCode);
        }
        return xav;
    }
    //end jaime

    public void visitAttribute(final Attribute attr) {
        buf.setLength(0);
        buf.append(tab).append("ATTRIBUTE ");
        appendDescriptor(-1, attr.type);

        if (attr instanceof Traceable) {
            ((Traceable) attr).trace(buf, labelNames);
        } else {
            buf.append(" : ").append(attr.toString()).append("\n");
        }

        text.add(buf.toString());
        if (mv != null) {
            mv.visitAttribute(attr);
        }
    }

    public AnnotationVisitor visitAnnotationDefault() {
        text.add(tab2 + "default=");
        TraceAnnotationVisitor tav = new TraceAnnotationVisitor();
        text.add(tav.getText());
        text.add("\n");
        if (mv != null) {
            tav.av = mv.visitAnnotationDefault();
        }
        return tav;
    }

    public AnnotationVisitor visitParameterAnnotation(
        final int parameter,
        final String desc,
        final boolean visible)
    {
        buf.setLength(0);
        buf.append(tab2).append('@');
        appendDescriptor(FIELD_DESCRIPTOR, desc);
        buf.append('(');
        text.add(buf.toString());
        TraceAnnotationVisitor tav = new TraceAnnotationVisitor();
        text.add(tav.getText());
        text.add(visible ? ") // parameter " : ") // invisible, parameter ");
        text.add(new Integer(parameter));
        text.add("\n");
        if (mv != null) {
            tav.av = mv.visitParameterAnnotation(parameter, desc, visible);
        }
        return tav;
    }

    public void visitCode() {
        if (mv != null) {
            mv.visitCode();
        }
    }

    public void visitInsn(final int opcode) {
        buf.setLength(0);
        buf.append(tab2).append(OPCODES[opcode]).append('\n');
        text.add(buf.toString());

        if (mv != null) {
            mv.visitInsn(opcode);
        }
    }

    public void visitIntInsn(final int opcode, final int operand) {
        buf.setLength(0);
        buf.append(tab2)
                .append(OPCODES[opcode])
                .append(' ')
                .append(opcode == Opcodes.NEWARRAY
                        ? TYPES[operand]
                        : Integer.toString(operand))
                .append('\n');
        text.add(buf.toString());

        if (mv != null) {
            mv.visitIntInsn(opcode, operand);
        }
    }

    public void visitVarInsn(final int opcode, final int var) {
        buf.setLength(0);
        buf.append(tab2)
                .append(OPCODES[opcode])
                .append(' ')
                .append(var)
                .append('\n');
        text.add(buf.toString());

        if (mv != null) {
            mv.visitVarInsn(opcode, var);
        }
    }

    public void visitTypeInsn(final int opcode, final String desc) {
        buf.setLength(0);
        buf.append(tab2).append(OPCODES[opcode]).append(' ');
        if (desc.startsWith("[")) {
            appendDescriptor(FIELD_DESCRIPTOR, desc);
        } else {
            appendDescriptor(INTERNAL_NAME, desc);
        }
        buf.append('\n');
        text.add(buf.toString());

        if (mv != null) {
            mv.visitTypeInsn(opcode, desc);
        }
    }

    public void visitFieldInsn(
        final int opcode,
        final String owner,
        final String name,
        final String desc)
    {
        buf.setLength(0);
        buf.append(tab2).append(OPCODES[opcode]).append(' ');
        appendDescriptor(INTERNAL_NAME, owner);
        buf.append('.').append(name).append(" : ");
        appendDescriptor(FIELD_DESCRIPTOR, desc);
        buf.append('\n');
        text.add(buf.toString());

        if (mv != null) {
            mv.visitFieldInsn(opcode, owner, name, desc);
        }
    }

    public void visitMethodInsn(
        final int opcode,
        final String owner,
        final String name,
        final String desc)
    {
        buf.setLength(0);
        buf.append(tab2).append(OPCODES[opcode]).append(' ');
        appendDescriptor(INTERNAL_NAME, owner);
        buf.append('.').append(name).append(' ');
        appendDescriptor(METHOD_DESCRIPTOR, desc);
        buf.append('\n');
        text.add(buf.toString());

        if (mv != null) {
            mv.visitMethodInsn(opcode, owner, name, desc);
        }
    }

    public void visitJumpInsn(final int opcode, final Label label) {
        buf.setLength(0);
        buf.append(tab2).append(OPCODES[opcode]).append(' ');
        appendLabel(label);
        buf.append('\n');
        text.add(buf.toString());

        if (mv != null) {
            mv.visitJumpInsn(opcode, label);
        }
    }

    public void visitLabel(final Label label) {
        buf.setLength(0);
        buf.append(ltab);
        appendLabel(label);
        buf.append('\n');
        text.add(buf.toString());

        if (mv != null) {
            mv.visitLabel(label);
        }
    }

    public void visitLdcInsn(final Object cst) {
        buf.setLength(0);
        buf.append(tab2).append("LDC ");
        if (cst instanceof String) {
            AbstractVisitor.appendString(buf, (String) cst);
        } else if (cst instanceof Type) {
            buf.append(((Type) cst).getDescriptor() + ".class");
        } else {
            buf.append(cst);
        }
        buf.append('\n');
        text.add(buf.toString());

        if (mv != null) {
            mv.visitLdcInsn(cst);
        }
    }

    public void visitIincInsn(final int var, final int increment) {
        buf.setLength(0);
        buf.append(tab2)
                .append("IINC ")
                .append(var)
                .append(' ')
                .append(increment)
                .append('\n');
        text.add(buf.toString());

        if (mv != null) {
            mv.visitIincInsn(var, increment);
        }
    }

    public void visitTableSwitchInsn(
        final int min,
        final int max,
        final Label dflt,
        final Label labels[])
    {
        buf.setLength(0);
        buf.append(tab2).append("TABLESWITCH\n");
        for (int i = 0; i < labels.length; ++i) {
            buf.append(tab3).append(min + i).append(": ");
            appendLabel(labels[i]);
            buf.append('\n');
        }
        buf.append(tab3).append("default: ");
        appendLabel(dflt);
        buf.append('\n');
        text.add(buf.toString());

        if (mv != null) {
            mv.visitTableSwitchInsn(min, max, dflt, labels);
        }
    }

    public void visitLookupSwitchInsn(
        final Label dflt,
        final int keys[],
        final Label labels[])
    {
        buf.setLength(0);
        buf.append(tab2).append("LOOKUPSWITCH\n");
        for (int i = 0; i < labels.length; ++i) {
            buf.append(tab3).append(keys[i]).append(": ");
            appendLabel(labels[i]);
            buf.append('\n');
        }
        buf.append(tab3).append("default: ");
        appendLabel(dflt);
        buf.append('\n');
        text.add(buf.toString());

        if (mv != null) {
            mv.visitLookupSwitchInsn(dflt, keys, labels);
        }
    }

    public void visitMultiANewArrayInsn(final String desc, final int dims) {
        buf.setLength(0);
        buf.append(tab2).append("MULTIANEWARRAY ");
        appendDescriptor(FIELD_DESCRIPTOR, desc);
        buf.append(' ').append(dims).append('\n');
        text.add(buf.toString());

        if (mv != null) {
            mv.visitMultiANewArrayInsn(desc, dims);
        }
    }

    public void visitTryCatchBlock(
        final Label start,
        final Label end,
        final Label handler,
        final String type)
    {
        buf.setLength(0);
        buf.append(tab2).append("TRYCATCHBLOCK ");
        appendLabel(start);
        buf.append(' ');
        appendLabel(end);
        buf.append(' ');
        appendLabel(handler);
        buf.append(' ');
        appendDescriptor(INTERNAL_NAME, type);
        buf.append('\n');
        text.add(buf.toString());

        if (mv != null) {
            mv.visitTryCatchBlock(start, end, handler, type);
        }
    }

    public void visitLocalVariable(
        final String name,
        final String desc,
        final String signature,
        final Label start,
        final Label end,
        final int index)
    {
        buf.setLength(0);
        buf.append(tab2).append("LOCALVARIABLE ").append(name).append(' ');
        appendDescriptor(FIELD_DESCRIPTOR, desc);
        buf.append(' ');
        appendLabel(start);
        buf.append(' ');
        appendLabel(end);
        buf.append(' ').append(index).append('\n');

        if (signature != null) {
            buf.append(tab2);
            appendDescriptor(FIELD_SIGNATURE, signature);

            TraceSignatureVisitor sv = new TraceSignatureVisitor(0);
            SignatureReader r = new SignatureReader(signature);
            r.acceptType(sv);
            buf.append(tab2)
                    .append("// declaration: ")
                    .append(sv.getDeclaration())
                    .append('\n');
        }
        text.add(buf.toString());

        if (mv != null) {
            mv.visitLocalVariable(name, desc, signature, start, end, index);
        }
    }

    public void visitLineNumber(final int line, final Label start) {
        buf.setLength(0);
        buf.append(tab2).append("LINENUMBER ").append(line).append(' ');
        appendLabel(start);
        buf.append('\n');
        text.add(buf.toString());

        if (mv != null) {
            mv.visitLineNumber(line, start);
        }
    }

    public void visitMaxs(final int maxStack, final int maxLocals) {
        buf.setLength(0);
        buf.append(tab2).append("MAXSTACK = ").append(maxStack).append('\n');
        text.add(buf.toString());

        buf.setLength(0);
        buf.append(tab2).append("MAXLOCALS = ").append(maxLocals).append('\n');
        text.add(buf.toString());

        if (mv != null) {
            mv.visitMaxs(maxStack, maxLocals);
        }
    }

    public void visitEnd() {
        super.visitEnd();

        if (mv != null) {
            mv.visitEnd();
        }
    }

    // ------------------------------------------------------------------------
    // Utility methods
    // ------------------------------------------------------------------------

    /**
     * Appends the name of the given label to {@link #buf buf}. Creates a new
     * label name if the given label does not yet have one.
     * 
     * @param l a label.
     */
    public void appendLabel(final Label l) {
        String name = (String) labelNames.get(l);
        if (name == null) {
            name = "L" + labelNames.size();
            labelNames.put(l, name);
        }
        buf.append(name);
    }
}
