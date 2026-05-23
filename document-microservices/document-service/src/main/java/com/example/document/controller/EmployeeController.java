package com.example.document.controller;

import com.example.document.client.EmployeeFeignClient;
import com.example.document.dto.ApiResponse;
import com.example.document.dto.EmployeeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller that exposes employee details retrieved from the external
 * insurance-claims-system application (http://localhost:8080) using a
 * Feign client.
 *
 * Base path: /api/employees
 */
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeFeignClient employeeFeignClient;

    /**
     * GET /api/employees
     * Calls the insurance-claims-system via Feign and returns all employees.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeDto>>> getAllEmployees() {
        List<EmployeeDto> employees = employeeFeignClient.getAllEmployees();
        return ResponseEntity.ok(new ApiResponse<>(
                true, "Employees retrieved from insurance-claims-system", employees));
    }

    /**
     * GET /api/employees/{id}
     * Calls the insurance-claims-system via Feign and returns a single employee.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeDto>> getEmployeeById(@PathVariable Long id) {
        EmployeeDto employee = employeeFeignClient.getEmployeeById(id);
        return ResponseEntity.ok(new ApiResponse<>(
                true, "Employee retrieved from insurance-claims-system", employee));
    }
}
