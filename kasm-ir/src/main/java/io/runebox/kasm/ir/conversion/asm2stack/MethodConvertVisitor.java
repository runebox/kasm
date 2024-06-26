package io.runebox.kasm.ir.conversion.asm2stack;

import io.runebox.kasm.ir.conversion.AccessConverter;
import io.runebox.kasm.ir.Classfile;
import io.runebox.kasm.ir.Method;
import io.runebox.kasm.ir.annotation.Annotation;
import io.runebox.kasm.ir.annotation.AnnotationValue;
import io.runebox.kasm.ir.typeannotation.MethodTypeAnnotation;
import io.runebox.kasm.ir.typeannotation.TargetType;
import io.runebox.kasm.ir.stack.StackBody;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.JSRInlinerAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Visitor that converts all events and stores them in a {@link Method}.
 */
public class MethodConvertVisitor extends JSRInlinerAdapter {
    private final Classfile classfile;
    private final Method method;
    private StackBody body;

    public MethodConvertVisitor(MethodVisitor methodVisitor, Classfile classfile, Method method, int access, String name, String descriptor, String signature, String[] exceptions) {
        super(Opcodes.ASM7, methodVisitor, access, name, descriptor, signature, exceptions);
        this.method = method;
        this.classfile = classfile;
    }

    @Override
    public void visitParameter(String name, int access) {
        super.visitParameter(name, access);

        Method.Parameter parameter = new Method.Parameter();
        parameter.setName(Optional.ofNullable(name));
        parameter.setFlags(AccessConverter.PARAMETER.fromBitMap(access));

        this.method.getParameterInfo().add(parameter);
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        AnnotationVisitor av = super.visitAnnotationDefault();
        av = new AbstractAnnotationConvertVisitor(av) {
            @Override
            public void visitConvertedAnnotationValue(String name, AnnotationValue value) {
                MethodConvertVisitor.this.method.setDefaultValue(Optional.of(value));
            }
        };
        return av;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        Annotation annotation = new Annotation(AsmUtil.pathFromObjectDescriptor(descriptor), visible);
        this.method.getAnnotations().add(annotation);

        AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
        av = new AnnotationConvertVisitor(av, annotation);
        return av;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        Annotation annotation = new Annotation(AsmUtil.pathFromObjectDescriptor(descriptor), visible);
        TargetType.MethodTargetType targetType = convertTargetType(new TypeReference(typeRef));
        this.method.getTypeAnnotations().add(new MethodTypeAnnotation(AsmUtil.fromAsmTypePath(typePath), annotation, targetType));

        AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
        av = new AnnotationConvertVisitor(av, annotation);
        return av;
    }

    private TargetType.MethodTargetType convertTargetType(TypeReference tref) {
        switch (tref.getSort()) {
            case TypeReference.THROWS:
                return new TargetType.CheckedException(tref.getExceptionIndex());

            case TypeReference.METHOD_FORMAL_PARAMETER:
                return new TargetType.MethodParameter(tref.getFormalParameterIndex());

            case TypeReference.METHOD_RECEIVER:
                return new TargetType.MethodReceiver();

            case TypeReference.METHOD_RETURN:
                return new TargetType.ReturnType();

            case TypeReference.METHOD_TYPE_PARAMETER:
                return new TargetType.TypeParameter(tref.getTypeParameterIndex());

            case TypeReference.METHOD_TYPE_PARAMETER_BOUND:
                return new TargetType.TypeParameterBound(tref.getTypeParameterIndex(), tref.getTypeParameterBoundIndex());

            default:
                throw new AssertionError();
        }
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
        List<List<Annotation>> parameterAnnotations = this.method.getParameterAnnotations();
        while (parameterAnnotations.size() <= parameter) {
            parameterAnnotations.add(new ArrayList<>());
        }

        Annotation annotation = new Annotation(AsmUtil.pathFromObjectDescriptor(descriptor), visible);
        parameterAnnotations.get(parameter).add(annotation);

        AnnotationVisitor av = super.visitParameterAnnotation(parameter, descriptor, visible);
        av = new AnnotationConvertVisitor(av, annotation);
        return av;
    }

    @Override
    public void visitAttribute(org.objectweb.asm.Attribute attribute) {
        method.getAttributes().add(new io.runebox.kasm.ir.Attribute(attribute.type, AsmUtil.getAttributeData(attribute)));
    }

    @Override
    public void visitCode() {
        super.visitCode();

        this.body = new StackBody();
        this.method.setBody(Optional.of(this.body));
    }

    @Override
    public void visitEnd() {
        super.visitEnd();

        if (this.body != null) {
            new BodyConverter(this.classfile, this.method, this.body, this).convert();
        }
    }
}
