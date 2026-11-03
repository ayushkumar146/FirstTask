package com.Odin.FirstTask.model;

import com.Odin.FirstTask.dto.AddressDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String name;
    private String email;
    private Integer age;
    private String role;
    private AddressDto address;
    private LocalDateTime createdAt;
}
