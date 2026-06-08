package com.clearticket.clearticket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSession implements Serializable {
    private String id;
    private String name;
    private String email;
}
