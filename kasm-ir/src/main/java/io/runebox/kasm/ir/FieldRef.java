package io.runebox.kasm.ir;

import io.runebox.kasm.ir.type.Type;

import java.util.Objects;

public class FieldRef {
    /**
     * Class that contains the field
     */
    private final io.runebox.kasm.ir.Path owner;

    /**
     * Name of the field
     */
    private final String name;

    /**
     * Type of the field
     */
    private final Type type;

    public FieldRef(io.runebox.kasm.ir.Path owner, String name, Type type) {
        this.owner = owner;
        this.name = name;
        this.type = type;
    }

    public io.runebox.kasm.ir.Path getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldRef fieldRef = (FieldRef) o;
        return Objects.equals(owner, fieldRef.owner) &&
                Objects.equals(name, fieldRef.name) &&
                Objects.equals(type, fieldRef.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, name, type);
    }

    @Override
    public String toString() {
        return FieldRef.class.getSimpleName() + '{' +
                "owner=" + owner +
                ", name='" + name + '\'' +
                ", type=" + type +
                '}';
    }
}
