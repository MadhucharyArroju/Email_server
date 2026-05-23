package com.example.document.client;

import com.example.document.dto.EmployeeDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Feign client that connects to the external "insurance-claims-system"
 * application to fetch employee details from its database.
 *
 * The target application runs at http://localhost:8080 (configurable via the
 * {@code insurance-claims-system.url} property). A fixed {@code url} is used
 * instead of Eureka service discovery, as requested.
 *
 * It is assumed the insurance-claims-system exposes:
 *   GET /api/employees        -> list of employees
 *   GET /api/employees/{id}   -> a single employee
 */
@FeignClient(
        name = "insurance-claims-system",
        url = "${insurance-claims-system.url:http://localhost:8080}"
)
public interface EmployeeFeignClient {

    /** Fetch all employees from the insurance-claims-system database. */
    @GetMapping("/api/employees")
    List<EmployeeDto> getAllEmployees();

    /** Fetch a single employee by id from the insurance-claims-system database. */
    @GetMapping("/api/employees/{id}")
    EmployeeDto getEmployeeById(@PathVariable("id") Long id);
}
