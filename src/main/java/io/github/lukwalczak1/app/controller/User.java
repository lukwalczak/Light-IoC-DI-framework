package io.github.lukwalczak1.app.controller;

import io.github.lukwalczak1.framework.container.validation.annotation.Max;
import io.github.lukwalczak1.framework.container.validation.annotation.Min;
import io.github.lukwalczak1.framework.container.validation.annotation.NotNull;
import io.github.lukwalczak1.framework.container.validation.annotation.Pattern;

public class User {

    @NotNull
    @Pattern("^[a-zA-Z]+$")
    private String name;

    @NotNull
    @Min(18)
    @Max(100)
    private int age;

    public User() {
    }

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
