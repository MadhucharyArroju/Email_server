package com.example.document.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentDTO {


    private Long departmentId;                          // ← Long

    private String departmentName;                      // unique IS valid here

    private Long departmentCode;                        // numeric — no length

    private String departmentDesc;
}
