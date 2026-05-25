package com.example.document.service;

import com.example.document.dto.AddressDTO;
import com.example.document.dto.DepartmentDTO;
import com.example.document.dto.EmployeeDto;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Builds a styled, Cigna-style "Dental Benefit Summary" PDF for a single
 * employee using the OpenPDF library.
 *
 * <p>The generated document reproduces the look-and-feel of a real insurance
 * benefit summary: branded header logos, green section banners, alternating
 * (zebra) row shading and cascading benefit-class tables.</p>
 *
 * <p>The logo artwork ({@code cigna.png}, {@code cigna2.png}) is loaded from
 * {@code src/main/resources/static/images}. The benefit-class figures are
 * generic standard-plan values driven by the employee record — replace them
 * with real plan data when integrating with a benefits source.</p>
 */
@Service
public class CignaPdfService {

    // -------------------- brand palette --------------------
    private static final Color GREEN_DARK = new Color(0, 119, 73);
    private static final Color GREEN_MID  = new Color(0, 148, 68);
    private static final Color GREEN_BAND = new Color(225, 240, 224);
    private static final Color ORANGE     = new Color(245, 130, 31);
    private static final Color NAVY       = new Color(0, 59, 92);
    private static final Color ZEBRA      = new Color(245, 248, 245);
    private static final Color WHITE      = Color.WHITE;
    private static final Color TEXT       = new Color(55, 55, 55);
    private static final Color BORDER     = new Color(200, 214, 203);

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MM/dd/yyyy");

    /** Build a {@link Font} in the Helvetica family. */
    private static Font f(float size, int style, Color color) {
        return new Font(Font.HELVETICA, size, style, color);
    }

    // =================================================================
    // Public API
    // =================================================================

    /**
     * Generate the Dental Benefit Summary PDF for the supplied employee.
     *
     * @param emp employee record (with department + addresses) to render
     * @return the rendered PDF as a byte array
     */
    public byte[] generateEmployeePdf(EmployeeDto emp) {
        Document document = new Document(PageSize.LETTER, 40, 40, 104, 66);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new HeaderFooter(
                    loadImage("static/images/cigna2.png"),   // top-left badge
                    loadImage("static/images/cigna.png")));   // top-right wordmark
            document.open();

            addTitleBlock(document, emp);
            addInsuredLine(document);
            addIntro(document);
            addPlanInformation(document, emp);
            addEmployeeSection(document, emp);
            addDepartmentSection(document, emp);
            addAddressSection(document, emp);
            addBenefitHighlights(document);
            addProvisions(document);

            document.close();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Failed to generate PDF: " + ex.getMessage(), ex);
        }
        return baos.toByteArray();
    }

    // =================================================================
    // Header / footer (drawn on every page)
    // =================================================================

    private static class HeaderFooter extends PdfPageEventHelper {

        private final Image leftLogo;
        private final Image rightLogo;

        HeaderFooter(Image leftLogo, Image rightLogo) {
            this.leftLogo = leftLogo;
            this.rightLogo = rightLogo;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Rectangle page = document.getPageSize();
            float top = page.getTop();
            float left = document.leftMargin();
            float right = page.getWidth() - document.rightMargin();

            // ----- branded logos -----
            try {
                if (leftLogo != null) {
                    Image l = Image.getInstance(leftLogo);
                    l.scaleToFit(150f, 46f);
                    l.setAbsolutePosition(left, top - 14f - l.getScaledHeight());
                    cb.addImage(l);
                }
                if (rightLogo != null) {
                    Image r = Image.getInstance(rightLogo);
                    r.scaleToFit(150f, 46f);
                    r.setAbsolutePosition(right - r.getScaledWidth(),
                            top - 14f - r.getScaledHeight());
                    cb.addImage(r);
                }
            } catch (Exception ignored) {
                // a missing logo must never break PDF generation
            }

            // ----- header rule (green + orange accent) -----
            float ruleY = top - 70f;
            cb.setColorStroke(GREEN_DARK);
            cb.setLineWidth(2.2f);
            cb.moveTo(left, ruleY);
            cb.lineTo(right, ruleY);
            cb.stroke();
            cb.setColorStroke(ORANGE);
            cb.setLineWidth(0.9f);
            cb.moveTo(left, ruleY - 3f);
            cb.lineTo(right, ruleY - 3f);
            cb.stroke();

            // ----- footer -----
            float footY = page.getBottom() + 30f;
            cb.setColorStroke(GREEN_DARK);
            cb.setLineWidth(0.8f);
            cb.moveTo(left, footY + 12f);
            cb.lineTo(right, footY + 12f);
            cb.stroke();

            Font ff = f(7.5f, Font.NORMAL, new Color(120, 120, 120));
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("Cigna Dental Benefit Summary  |  Confidential", ff),
                    left, footY, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase("Page " + writer.getPageNumber(), ff),
                    (left + right) / 2f, footY, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("Generated by document-service", ff),
                    right, footY, 0);
        }
    }

    // =================================================================
    // Document sections
    // =================================================================

    /** Green title banner with the plan-sponsor sub-line. */
    private void addTitleBlock(Document doc, EmployeeDto emp) throws DocumentException {
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        banner.setSpacingBefore(2f);

        PdfPCell title = new PdfPCell(new Phrase(
                "DENTAL BENEFIT SUMMARY", f(20f, Font.BOLD, WHITE)));
        title.setBackgroundColor(GREEN_DARK);
        title.setHorizontalAlignment(Element.ALIGN_CENTER);
        title.setPadding(11f);
        title.setBorder(Rectangle.NO_BORDER);
        banner.addCell(title);

        PdfPCell sub = new PdfPCell(new Phrase(
                "Group Benefit Plan  •  " + planSponsor(emp)
                        + "  •  Plan Renewal Date: " + renewalDate(),
                f(9.5f, Font.BOLD, NAVY)));
        sub.setBackgroundColor(GREEN_BAND);
        sub.setHorizontalAlignment(Element.ALIGN_CENTER);
        sub.setPadding(6f);
        sub.setBorder(Rectangle.NO_BORDER);
        banner.addCell(sub);

        doc.add(banner);
    }

    /** Centred "Insured by" line, as seen on real benefit summaries. */
    private void addInsuredLine(Document doc) throws DocumentException {
        Paragraph p = new Paragraph(
                "Insured by Cigna Health and Life Insurance Company",
                f(8.5f, Font.ITALIC, new Color(110, 110, 110)));
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(6f);
        p.setSpacingAfter(2f);
        doc.add(p);
    }

    /** Short explanatory note inside a light info box. */
    private void addIntro(Document doc) throws DocumentException {
        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(100);
        box.setSpacingBefore(6f);

        PdfPCell c = new PdfPCell(new Phrase(
                "This material is for informational purposes only and is designed to "
                        + "highlight some of the benefits available under the plan. It is "
                        + "not a contract. Actual covered services, reimbursement levels, "
                        + "exclusions and limitations are governed by the plan documents. "
                        + "If there is any difference between this summary and the official "
                        + "plan documents, the plan documents will govern.",
                f(8f, Font.NORMAL, TEXT)));
        c.setBackgroundColor(new Color(250, 250, 248));
        c.setBorderColor(BORDER);
        c.setBorderWidth(0.6f);
        c.setPadding(8f);
        box.addCell(c);

        doc.add(box);
    }

    /** Plan-level information grid (plan name, group no, tier, dates). */
    private void addPlanInformation(Document doc, EmployeeDto emp) throws DocumentException {
        PdfPTable t = baseTable(new float[]{1.2f, 1.8f, 1.2f, 1.8f});
        t.addCell(sectionBar("PLAN INFORMATION", 4));

        t.addCell(keyCell("Plan Name"));
        t.addCell(valCell("Cigna Dental — Gold DPPO"));
        t.addCell(keyCell("Network"));
        t.addCell(valCell("Total Cigna DPPO Network"));

        t.addCell(keyCell("Group / Policy No."));
        t.addCell(valCell(groupNumber(emp)));
        t.addCell(keyCell("Coverage Tier"));
        t.addCell(valCell("Employee + Family"));

        t.addCell(keyCell("Effective Date"));
        t.addCell(valCell(effectiveDate()));
        t.addCell(keyCell("Plan Renewal Date"));
        t.addCell(valCell(renewalDate()));

        doc.add(t);
    }

    /** Employee identity / payroll details. */
    private void addEmployeeSection(Document doc, EmployeeDto emp) throws DocumentException {
        PdfPTable t = baseTable(new float[]{1.2f, 1.8f, 1.2f, 1.8f});
        t.addCell(sectionBar("EMPLOYEE DETAILS", 4));

        t.addCell(keyCell("Employee ID"));
        t.addCell(valCell(emp.getEmpId() == null ? null : String.valueOf(emp.getEmpId())));
        t.addCell(keyCell("Full Name"));
        t.addCell(valCell(emp.getEmpName()));

        t.addCell(keyCell("Age"));
        t.addCell(valCell(emp.getAge() == null ? null : emp.getAge() + " years"));
        t.addCell(keyCell("Annual Salary"));
        t.addCell(valCell(money(emp.getSalary())));

        t.addCell(keyCell("Department"));
        PdfPCell dept = valCell(departmentName(emp));
        dept.setColspan(3);
        t.addCell(dept);

        doc.add(t);
    }

    /** Department details pulled from the employee record. */
    private void addDepartmentSection(Document doc, EmployeeDto emp) throws DocumentException {
        PdfPTable t = baseTable(new float[]{1.2f, 1.8f, 1.2f, 1.8f});
        t.addCell(sectionBar("DEPARTMENT DETAILS", 4));

        DepartmentDTO d = firstDepartment(emp);
        if (d == null) {
            PdfPCell none = valCell("No department information on record.");
            none.setColspan(4);
            none.setHorizontalAlignment(Element.ALIGN_CENTER);
            t.addCell(none);
        } else {
            t.addCell(keyCell("Department Name"));
            t.addCell(valCell(d.getDepartmentName()));
            t.addCell(keyCell("Department Code"));
            t.addCell(valCell(d.getDepartmentCode() == null
                    ? null : String.valueOf(d.getDepartmentCode())));

            t.addCell(keyCell("Description"));
            PdfPCell desc = valCell(d.getDepartmentDesc());
            desc.setColspan(3);
            t.addCell(desc);
        }
        doc.add(t);
    }

    /** One row per registered address (cascading list). */
    private void addAddressSection(Document doc, EmployeeDto emp) throws DocumentException {
        PdfPTable t = baseTable(new float[]{0.5f, 2.4f, 1.6f, 1.4f, 1.1f});
        t.addCell(sectionBar("REGISTERED ADDRESS(ES)", 5));

        t.addCell(colHead("#"));
        t.addCell(colHead("Street"));
        t.addCell(colHead("City"));
        t.addCell(colHead("State"));
        t.addCell(colHead("Pincode"));

        List<AddressDTO> addresses = emp.getAddresses();
        if (addresses == null || addresses.isEmpty()) {
            PdfPCell none = dataCell("No address on record for this employee.",
                    Element.ALIGN_CENTER, WHITE, Font.ITALIC);
            none.setColspan(5);
            t.addCell(none);
        } else {
            int i = 1;
            for (AddressDTO a : addresses) {
                Color bg = (i % 2 == 0) ? ZEBRA : WHITE;
                t.addCell(dataCell(String.valueOf(i), Element.ALIGN_CENTER, bg, Font.BOLD));
                t.addCell(dataCell(a.getStreet(), Element.ALIGN_LEFT, bg, Font.NORMAL));
                t.addCell(dataCell(a.getCity(), Element.ALIGN_LEFT, bg, Font.NORMAL));
                t.addCell(dataCell(a.getState(), Element.ALIGN_LEFT, bg, Font.NORMAL));
                t.addCell(dataCell(a.getPincode(), Element.ALIGN_CENTER, bg, Font.NORMAL));
                i++;
            }
        }
        doc.add(t);
    }

    /**
     * Cascading benefit-class table styled like a real DPPO summary.
     * Group rows split the table into "Plan Maximums &amp; Deductible" and
     * the Class&nbsp;I&ndash;IX coverage tiers.
     */
    private void addBenefitHighlights(Document doc) throws DocumentException {
        PdfPTable t = baseTable(new float[]{2.6f, 1.4f, 1.4f});
        t.addCell(sectionBar("DENTAL PLAN BENEFIT HIGHLIGHTS — DPPO", 3));

        t.addCell(colHead("Coverage Detail"));
        t.addCell(colHead("In-Network\nPlan Pays"));
        t.addCell(colHead("Out-of-Network\nPlan Pays"));

        // ---- group: plan maximums & deductible ----
        t.addCell(groupRow("Plan Maximums & Deductible", 3));
        benefitRow(t, "Calendar Year Benefit Maximum", "$2,000", "$2,000", false);
        benefitRow(t, "Calendar Year Deductible — Individual", "$50", "$50", true);
        benefitRow(t, "Calendar Year Deductible — Family", "$150", "$150", false);
        benefitRow(t, "Orthodontia Lifetime Maximum", "$1,500", "$1,500", true);

        // ---- group: class I ----
        t.addCell(groupRow("Class I — Diagnostic & Preventive", 3));
        benefitRow(t, "Oral evaluations, routine cleanings, X-rays", "100%", "80%", false);
        benefitRow(t, "Fluoride application, sealants, space maintainers", "100%", "80%", true);

        // ---- group: class II ----
        t.addCell(groupRow("Class II — Basic Restorative", 3));
        benefitRow(t, "Fillings (amalgam & composite)", "80%", "60%", false);
        benefitRow(t, "Simple extractions & oral surgery", "80%", "60%", true);
        benefitRow(t, "Endodontics (root canal) & periodontics", "80%", "60%", false);

        // ---- group: class III ----
        t.addCell(groupRow("Class III — Major Restorative", 3));
        benefitRow(t, "Crowns, inlays & onlays", "50%", "50%", true);
        benefitRow(t, "Bridges, dentures & prosthetics repair", "50%", "50%", false);

        // ---- group: class IV ----
        t.addCell(groupRow("Class IV — Orthodontia", 3));
        benefitRow(t, "Orthodontic treatment (dependent children)", "50%", "50%", true);

        // ---- group: class IX ----
        t.addCell(groupRow("Class IX — Implants", 3));
        benefitRow(t, "Surgical placement & implant restoration", "50%", "50%", false);

        doc.add(t);
    }

    /** Closing provisions / notes block. */
    private void addProvisions(Document doc) throws DocumentException {
        PdfPTable t = baseTable(new float[]{1f});
        t.addCell(sectionBar("PLAN PROVISIONS & NOTES", 1));

        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(WHITE);
        c.setBorderColor(BORDER);
        c.setBorderWidth(0.6f);
        c.setPadding(8f);
        c.addElement(bullet("Deductibles apply to Class II, III and IX services; "
                + "Class I preventive care is not subject to the deductible."));
        c.addElement(bullet("In-network benefits are based on contracted fees; "
                + "out-of-network benefits are based on the maximum reimbursable charge."));
        c.addElement(bullet("Benefit percentages indicate the share of covered "
                + "expenses paid by the plan after any applicable deductible."));
        c.addElement(bullet("Orthodontia coverage is limited to eligible dependent "
                + "children and is subject to the lifetime maximum shown above."));
        t.addCell(c);

        doc.add(t);

        Paragraph foot = new Paragraph(
                "This Dental Benefit Summary was generated electronically by the "
                        + "document-service application and reflects employee data retrieved "
                        + "from the insurance-claims-system. It is provided for reference "
                        + "only and does not constitute a guarantee of coverage.",
                f(7.5f, Font.ITALIC, new Color(125, 125, 125)));
        foot.setAlignment(Element.ALIGN_CENTER);
        foot.setSpacingBefore(10f);
        doc.add(foot);
    }

    // =================================================================
    // Cell / table helpers
    // =================================================================

    private PdfPTable baseTable(float[] widths) throws DocumentException {
        PdfPTable t = new PdfPTable(widths.length);
        t.setWidths(widths);
        t.setWidthPercentage(100);
        t.setSpacingBefore(11f);
        t.setSpacingAfter(2f);
        return t;
    }

    /** Full-width green section banner. */
    private PdfPCell sectionBar(String text, int colspan) {
        PdfPCell c = new PdfPCell(new Phrase(text, f(10.5f, Font.BOLD, WHITE)));
        c.setBackgroundColor(GREEN_DARK);
        c.setColspan(colspan);
        c.setPadding(6f);
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    /** Bold key cell on a light-green band. */
    private PdfPCell keyCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, f(9f, Font.BOLD, NAVY)));
        c.setBackgroundColor(GREEN_BAND);
        c.setPadding(5f);
        c.setBorderColor(BORDER);
        c.setBorderWidth(0.6f);
        return c;
    }

    /** Plain value cell. */
    private PdfPCell valCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(
                (text == null || text.trim().isEmpty()) ? "—" : text,
                f(9f, Font.NORMAL, TEXT)));
        c.setBackgroundColor(WHITE);
        c.setPadding(5f);
        c.setBorderColor(BORDER);
        c.setBorderWidth(0.6f);
        return c;
    }

    /** Centred white-on-green column header. */
    private PdfPCell colHead(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, f(8.5f, Font.BOLD, WHITE)));
        c.setBackgroundColor(GREEN_MID);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(5f);
        c.setBorderColor(BORDER);
        c.setBorderWidth(0.6f);
        return c;
    }

    /** Generic data cell with alignment / background / style control. */
    private PdfPCell dataCell(String text, int align, Color bg, int style) {
        PdfPCell c = new PdfPCell(new Phrase(
                (text == null || text.trim().isEmpty()) ? "—" : text,
                f(8.8f, style, TEXT)));
        c.setBackgroundColor(bg);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(5f);
        c.setBorderColor(BORDER);
        c.setBorderWidth(0.6f);
        return c;
    }

    /** Cascading sub-section row inside the benefit table. */
    private PdfPCell groupRow(String text, int colspan) {
        PdfPCell c = new PdfPCell(new Phrase(text, f(9f, Font.BOLD, GREEN_DARK)));
        c.setBackgroundColor(GREEN_BAND);
        c.setColspan(colspan);
        c.setPadding(5f);
        c.setBorderColor(BORDER);
        c.setBorderWidth(0.6f);
        return c;
    }

    /** One coverage line inside the benefit table. */
    private void benefitRow(PdfPTable t, String label, String inNet,
                            String outNet, boolean zebra) {
        Color bg = zebra ? ZEBRA : WHITE;
        t.addCell(dataCell(label, Element.ALIGN_LEFT, bg, Font.NORMAL));
        t.addCell(dataCell(inNet, Element.ALIGN_CENTER, bg, Font.BOLD));
        t.addCell(dataCell(outNet, Element.ALIGN_CENTER, bg, Font.NORMAL));
    }

    /** A small bulleted paragraph for the provisions box. */
    private Paragraph bullet(String text) {
        Paragraph p = new Paragraph("•  " + text, f(8f, Font.NORMAL, TEXT));
        p.setSpacingAfter(3f);
        p.setLeading(11f);
        return p;
    }

    // =================================================================
    // Data helpers
    // =================================================================

    private Image loadImage(String classpathLocation) {
        try {
            ClassPathResource res = new ClassPathResource(classpathLocation);
            if (!res.exists()) {
                return null;
            }
            byte[] bytes = StreamUtils.copyToByteArray(res.getInputStream());
            return Image.getInstance(bytes);
        } catch (Exception ex) {
            // a missing/unreadable logo must never break PDF generation
            return null;
        }
    }

    private DepartmentDTO firstDepartment(EmployeeDto emp) {
        if (emp.getDepartment() != null && !emp.getDepartment().isEmpty()) {
            return emp.getDepartment().get(0);
        }
        return null;
    }

    private String departmentName(EmployeeDto emp) {
        DepartmentDTO d = firstDepartment(emp);
        return (d == null) ? null : d.getDepartmentName();
    }

    private String planSponsor(EmployeeDto emp) {
        String name = departmentName(emp);
        return (name == null || name.trim().isEmpty()) ? "Cigna Group Benefits" : name;
    }

    private String groupNumber(EmployeeDto emp) {
        DepartmentDTO d = firstDepartment(emp);
        if (d != null && d.getDepartmentCode() != null) {
            return "GRP-" + d.getDepartmentCode();
        }
        if (emp.getEmpId() != null) {
            return "GRP-" + String.format("%06d", emp.getEmpId());
        }
        return "GRP-000000";
    }

    private String money(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        return "$ " + new DecimalFormat("#,##0.00").format(value);
    }

    private String effectiveDate() {
        return LocalDate.of(LocalDate.now().getYear(), 1, 1).format(DATE_FMT);
    }

    private String renewalDate() {
        return LocalDate.of(LocalDate.now().getYear() + 1, 1, 1).format(DATE_FMT);
    }
}
