package com.example.document.controller;

import com.example.document.dto.ApiResponse;
import com.example.document.dto.EmployeeDto;
import com.example.document.exception.ResourceNotFoundException;
import com.example.document.service.CignaPdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * REST controller that generates a styled, Cigna-style "Dental Benefit Summary"
 * PDF for an employee.
 *
 * <p>On input of an employee id (e.g. {@code id=1}) it retrieves the employee,
 * address and department data and renders a branded PDF with the
 * <strong>cigna</strong> logo on the top-right and the <strong>cigna2</strong>
 * logo on the top-left.</p>
 *
 * <p>Employee data is obtained by <strong>reusing the already-developed
 * {@link EmployeeController}</strong> in this same package: its
 * {@code getAllEmployees()} method ({@code GET /api/employees}) fetches every
 * employee from the external insurance-claims-system database via the Feign
 * client. This controller filters that list for the requested id, so no data
 * access logic is duplicated here.</p>
 *
 * Base path: {@code /api/pdf}
 */
@RestController
@RequestMapping("/api/pdf")
public class PDFController {

    /** Reused existing controller — its getAllEmployees() is the data source. */
    @Autowired
    private EmployeeController employeeController;

    /** Service that builds the branded PDF document. */
    @Autowired
    private CignaPdfService cignaPdfService;

    // ----------------------------------------------------------------
    // GET /api/pdf/employee/{id}
    // ----------------------------------------------------------------

    /**
     * Generate the Dental Benefit Summary PDF for a single employee.
     *
     * <p>Example: {@code GET /api/pdf/employee/1} returns the PDF for the
     * employee whose id is {@code 1}.</p>
     *
     * @param id       the employee id (Employee, Address and Department data)
     * @param download when {@code true} the browser is told to download the
     *                 file; otherwise it is displayed inline
     * @return the generated PDF as {@code application/pdf}
     */
    @GetMapping("/employee/{id}")
    public ResponseEntity<byte[]> generateEmployeePdf(
            @PathVariable Long id,
            @RequestParam(name = "download", defaultValue = "false") boolean download) {

        EmployeeDto employee = findEmployee(id);
        byte[] pdf = cignaPdfService.generateEmployeePdf(employee);

        String fileName = "Cigna-Dental-Benefit-Summary-Employee-" + id + ".pdf";
        String disposition = (download ? "attachment" : "inline")
                + "; filename=\"" + fileName + "\"";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, disposition);
        headers.setContentLength(pdf.length);

        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    // ----------------------------------------------------------------
    // GET /api/pdf/employees   (helper - lists ids available for a PDF)
    // ----------------------------------------------------------------

    /**
     * Convenience endpoint that returns every employee for which a PDF can be
     * generated. Useful for discovering valid ids before calling
     * {@code /api/pdf/employee/{id}}.
     */
    @GetMapping("/employees")
    public ResponseEntity<ApiResponse<List<EmployeeDto>>> listEligibleEmployees() {
        return ResponseEntity.ok(new ApiResponse<>(
                true, "Employees available for PDF generation", allEmployees()));
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    /**
     * Reuse {@link EmployeeController#getAllEmployees()} and locate the
     * employee with the requested id.
     */
    private EmployeeDto findEmployee(Long id) {
        return allEmployees().stream()
                .filter(e -> e != null && id.equals(e.getEmpId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + id
                                + ". Ensure the insurance-claims-system is running "
                                + "on http://localhost:8080."));
    }

    /** Pull the full employee list from the reused EmployeeController. */
    private List<EmployeeDto> allEmployees() {
        ResponseEntity<ApiResponse<List<EmployeeDto>>> response =
                employeeController.getAllEmployees();
        ApiResponse<List<EmployeeDto>> body =
                (response == null) ? null : response.getBody();
        List<EmployeeDto> employees = (body == null) ? null : body.getData();
        return (employees == null) ? Collections.emptyList() : employees;
    }
}
