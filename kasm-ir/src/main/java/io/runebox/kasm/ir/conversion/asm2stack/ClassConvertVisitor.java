package io.runebox.kasm.ir.conversion.asm2stack;

import io.runebox.kasm.ir.Module;
import io.runebox.kasm.ir.conversion.AccessConverter;
import io.runebox.kasm.ir.Attribute;
import io.runebox.kasm.ir.*;
import io.runebox.kasm.ir.annotation.Annotation;
import io.runebox.kasm.ir.constant.*;
import io.runebox.kasm.ir.typeannotation.ClassTypeAnnotation;
import io.runebox.kasm.ir.typeannotation.TargetType;
import org.objectweb.asm.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link ClassVisitor} that creates a {@link Classfile} from the visited events.
 */
public class ClassConvertVisitor extends ClassVisitor {
    private Classfile classfile;

    public ClassConvertVisitor() {
        super(Opcodes.ASM7);
    }

    public Classfile getClassfile() {
        return classfile;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        Classfile.Version convertedVersion = parseVersion(version);

        Path convertedSuperType = superName == null ? null : AsmUtil.pathFromInternalName(superName);

        List<Path> convertedInterfaces = (interfaces == null ? Stream.<String>empty() : Arrays.stream(interfaces))
                .map(AsmUtil::pathFromInternalName)
                .collect(Collectors.toList());

        this.classfile = new Classfile(convertedVersion, AsmUtil.pathFromInternalName(name), convertedSuperType, convertedInterfaces);
        this.classfile.setSignature(Optional.ofNullable(signature));
        this.classfile.getFlags().addAll(AccessConverter.CLASSFILE.fromBitMap(access));
    }

    private Classfile.Version parseVersion(int version) {
        int major = version & 0xFFFF;
        int minor = (version >>> 16) & 0xFFFF;
        return new Classfile.Version(major, minor);
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);

        this.classfile.setSource(Optional.ofNullable(source));
        this.classfile.setSourceDebug(Optional.ofNullable(debug));
    }

    @Override
    public ModuleVisitor visitModule(String name, int access, String version) {
        Module module = new Module(AsmUtil.pathFromModuleName(name));
        module.setVersion(Optional.ofNullable(version));
        module.getAccessFlags().addAll(AccessConverter.MODULE.fromBitMap(access));
        this.classfile.setModule(Optional.of(module));

        ModuleVisitor mv = super.visitModule(name, access, version);
        mv = new ModuleConvertVisitor(module, mv);
        return mv;
    }

    @Override
    public void visitNestHost(String nestHost) {
        super.visitNestHost(nestHost);

        this.classfile.setNestHost(Optional.of(AsmUtil.pathFromInternalName(nestHost)));
    }

    @Override
    public void visitOuterClass(String owner, String name, String descriptor) {
        super.visitOuterClass(owner, name, descriptor);
        Path ownerPath = AsmUtil.pathFromInternalName(owner);
        Optional<MethodDescriptor> methodDescOpt = Optional.ofNullable(descriptor).map(AsmUtil::parseMethodDescriptor);

        this.classfile.setEnclosingMethod(Optional.of(new Classfile.EnclosingMethod(ownerPath, Optional.ofNullable(name), methodDescOpt)));
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        Annotation annotation = new Annotation(AsmUtil.pathFromObjectDescriptor(descriptor), visible);
        this.classfile.getAnnotations().add(annotation);

        AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
        av = new AnnotationConvertVisitor(av, annotation);
        return av;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        Annotation annotation = new Annotation(AsmUtil.pathFromObjectDescriptor(descriptor), visible);
        TargetType.ClassTargetType targetType = convertClassTargetType(typeRef);
        this.classfile.getTypeAnnotations().add(new ClassTypeAnnotation(AsmUtil.fromAsmTypePath(typePath), annotation, targetType));

        AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
        av = new AnnotationConvertVisitor(av, annotation);
        return av;
    }

    private TargetType.ClassTargetType convertClassTargetType(int typeRef) {
        TypeReference tref = new TypeReference(typeRef);
        switch (tref.getSort()) {
            case TypeReference.CLASS_TYPE_PARAMETER:
                return new TargetType.TypeParameter(tref.getTypeParameterIndex());

            case TypeReference.CLASS_TYPE_PARAMETER_BOUND:
                return new TargetType.TypeParameterBound(tref.getTypeParameterIndex(), tref.getTypeParameterBoundIndex());

            case TypeReference.CLASS_EXTENDS:
                int interfaceIndex = tref.getSuperTypeIndex();
                return interfaceIndex == -1 ? new TargetType.Extends() :
                        new TargetType.Implements(interfaceIndex);

            default:
                throw new AssertionError();
        }
    }

    @Override
    public void visitAttribute(org.objectweb.asm.Attribute attribute) {
        super.visitAttribute(attribute);

        this.classfile.getAttributes().add(new Attribute(attribute.type, AsmUtil.getAttributeData(attribute)));
    }

    @Override
    public void visitNestMember(String nestMember) {
        super.visitNestMember(nestMember);

        this.classfile.getNestMembers().add(AsmUtil.pathFromInternalName(nestMember));
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        super.visitInnerClass(name, outerName, innerName, access);

        this.classfile.getInnerClasses().add(new Classfile.InnerClass(
                AsmUtil.pathFromInternalName(name),
                Optional.ofNullable(outerName).map(AsmUtil::pathFromInternalName),
                Optional.ofNullable(innerName),
                AccessConverter.INNER_CLASS.fromBitMap(access)));
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        Field field = new Field(name, AsmUtil.fromAsmType(Type.getType(descriptor)));
        field.getFlags().addAll(AccessConverter.FIELD.fromBitMap(access));
        field.setSignature(Optional.ofNullable(signature));
        field.setValue(Optional.ofNullable(value).map(this::convertFieldValue));

        this.classfile.getFields().add(field);

        FieldVisitor fv = super.visitField(access, name, descriptor, signature, value);
        fv = new FieldConvertVisitor(fv, field);
        return fv;
    }

    private FieldConstant convertFieldValue(Object value) {
        if (value instanceof Integer) {
            return new IntConstant((Integer) value);
        } else if (value instanceof Long) {
            return new LongConstant((Long) value);
        } else if (value instanceof Float) {
            return new FloatConstant((Float) value);
        } else if (value instanceof Double) {
            return new DoubleConstant((Double) value);
        } else if (value instanceof String) {
            return new StringConstant((String) value);
        } else {
            throw new AssertionError();
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodDescriptor desc = AsmUtil.parseMethodDescriptor(descriptor);
        Method method = new Method(name, desc.getParameterTypes(), desc.getReturnType());
        method.getFlags().addAll(AccessConverter.METHOD.fromBitMap(access));
        method.setSignature(Optional.ofNullable(signature));

        (exceptions == null ? Stream.<String>empty() : Arrays.stream(exceptions))
                .map(AsmUtil::pathFromInternalName)
                .forEach(method.getExceptions()::add);

        this.classfile.getMethods().add(method);

        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        mv = new MethodConvertVisitor(mv, classfile, method, access, name, descriptor, signature, exceptions);
        return mv;
    }
}
