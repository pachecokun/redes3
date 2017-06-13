/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mx.classes;

public class User {

    private final String user;
    private final String pass;
    private final Authorization_Level level;

    public User(String user, String pass, Authorization_Level level) {
        this.user = user;
        this.pass = pass;
        this.level = level;
    }
}
