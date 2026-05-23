package com.example.document.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data transfer object representing an employee record fetched from the
 * external insurance-claims-system application via the Feign client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDto {

    private Long empId;

    private String empName;

    private BigDecimal salary;

    private Integer age;




    private List<AddressDTO> addresses;


    private List<DepartmentDTO> department;
}
