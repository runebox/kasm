package io.runebox.kasm.ir;

import io.runebox.kasm.ir.annotation.Annotation;
import io.runebox.kasm.ir.constant.FieldConstant;
import io.runebox.kasm.ir.type.Type;
import io.runebox.kasm.ir.typeannotation.FieldTypeAnnotation;

import java.util.*;

/**
 * A field definition within a {@link Classfile}.
 */
public class Field {
    /**
     * Access flags of the field
     */
    private Set<Flag> flags;

    /**
     * Name of the field
     */
    private String name;

    /**
     * Type of values that can be stored in the field.
     */
    private Type type;

    /**
     * Type of the field with type variables.
     */
    private Optional<String> signature;

    /**
     * The initial value of a static field of primitive or {@link String} type.
     */
    private Optional<FieldConstant> value;

    /**
     * Annotations of this field.
     */
    private List<Annotation> annotations;

    /**
     * Type annotations on the type of this field.
     */
    private List<FieldTypeAnnotation> typeAnnotations;

    /**
     * Non-parsed attributes of this field.
     *
     * They are either not part of the JVM spec or
     * are not yet supported by this library.
     */
    private List<Attribute> attributes;

    public Field(String name, Type type) {
        this(new HashSet<>(), name, type, Optional.empty(), Optional.empty(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public Field(Set<Flag> flags, String name, Type type, Optional<String> signature, Optional<FieldConstant> value, List<Annotation> annotations, List<FieldTypeAnnotation> typeAnnotations, List<Attribute> attributes) {
        this.flags = flags;
        this.name = name;
        this.type = type;
        this.signature = signature;
        this.value = value;
        this.annotations = annotations;
        this.typeAnnotations = typeAnnotations;
        this.attributes = attributes;
    }

    public Set<Flag> getFlags() {
        return flags;
    }

    public void setFlags(Set<Flag> flags) {
        this.flags = flags;
    }

    /**
     * Check whether a {@link Flag} is set for this field.
     *
     * @param flag flag to check for
     * @return is the flag set
     */
    public boolean getFlag(Flag flag) {
        return flags.contains(flag);
    }

    /**
     * Set/remove a {@link Flag}.
     *
     * @param flag to be set/removed
     * @param shouldSet should the flag be set or removed
     */
    public void setFlag(Flag flag, boolean shouldSet) {
        if (shouldSet) {
            flags.add(flag);
        } else {
            flags.remove(flag);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Optional<String> getSignature() {
        return signature;
    }

    public void setSignature(Optional<String> signature) {
        this.signature = signature;
    }

    public Optional<FieldConstant> getValue() {
        return value;
    }

    public void setValue(Optional<FieldConstant> value) {
        this.value = value;
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
    }

    public List<FieldTypeAnnotation> getTypeAnnotations() {
        return typeAnnotations;
    }

    public void setTypeAnnotations(List<FieldTypeAnnotation> typeAnnotations) {
        this.typeAnnotations = typeAnnotations;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Field field = (Field) o;
        return Objects.equals(flags, field.flags) &&
                Objects.equals(name, field.name) &&
                Objects.equals(type, field.type) &&
                Objects.equals(signature, field.signature) &&
                Objects.equals(value, field.value) &&
                Objects.equals(annotations, field.annotations) &&
                Objects.equals(typeAnnotations, field.typeAnnotations) &&
                Objects.equals(attributes, field.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flags, name, type, signature, value, annotations, typeAnnotations, attributes);
    }

    @Override
    public String toString() {
        return Field.class.getSimpleName() + '{' +
                "flags=" + flags +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", signature=" + signature +
                ", value=" + value +
                ", annotations=" + annotations +
                ", typeAnnotations=" + typeAnnotations +
                ", attributes=" + attributes +
                '}';
    }

    public static enum Flag {
        PUBLIC, PRIVATE, PROTECTED, STATIC, FINAL, VOLATILE, TRANSIENT, SYNTHETIC, ENUM
    }
}
