package com.losobie.annotations;

import java.util.Objects;

public class TestObject {

    private int id;
    private String name;

    public int getId() {
        return id;
    }

    @BuilderMethod
    public void setId( int id ) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    @BuilderMethod
    public void setName( String name ) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + this.id;
        hash = 43 * hash + Objects.hashCode( this.name );
        return hash;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( obj == null ) {
            return false;
        }
        if ( getClass() != obj.getClass() ) {
            return false;
        }
        final TestObject other = (TestObject) obj;
        if ( this.id != other.id ) {
            return false;
        }
        return Objects.equals( this.name, other.name );
    }

    @Override
    public String toString() {
        return "TestObject{" + "id=" + id + ", name=" + name + '}';
    }

}
