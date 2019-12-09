package org.servantscode.donation;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class PdfWriter implements Closeable {

    private static final float LINE_SPACING = 1.25f;
    private static float MARGIN_Y = 80;
    private static float MARGIN_X = 60;

    private PDDocument document;

    private PDPage currentPage;
    private PDPageContentStream content;
    private float pageWidth = 0;

    private static final PDFont FONT = PDType1Font.TIMES_ROMAN;
    private float currentFontSize = 12;
    private float currentLeading = LINE_SPACING*12;


    private int[] tableColumnWidths = new int[0];
    private boolean[] tableAlignments = new boolean[0];

    float currentX = 0f;
    float currentY = 0f;

    public PdfWriter() throws IOException {
        document = new PDDocument();
        newPage();
    }

    public void newPage() throws IOException {
        closeContent();

        currentPage = new PDPage();
        document.addPage(currentPage);
        content = new PDPageContentStream(document, currentPage);

        PDRectangle mediaBox = currentPage.getMediaBox();
        pageWidth = mediaBox.getWidth() - MARGIN_X*2;
        currentX =  mediaBox.getLowerLeftX() + MARGIN_X;
        currentY =  mediaBox.getUpperRightY() - MARGIN_Y;
    }

    public void beginText() throws IOException {
        content.beginText();
        content.newLineAtOffset(currentX, currentY);
        content.setFont(FONT, currentFontSize);
        content.setLeading(currentLeading);
    }

    public void endText() throws IOException {
        content.endText();
    }

    public void setFontSize(int size) throws IOException {
        currentFontSize = size;
        currentLeading = LINE_SPACING*size;
        content.setFont(FONT, size);
        content.setLeading(currentLeading);
    }

    public void addLine(String text) throws IOException {
        content.showText(text);
        content.newLine();
    }

    public void addCenteredLine(String line) throws IOException {
        float titleWidth = calculateWidth(line);
        float xStart = (pageWidth - titleWidth) / 2;
        content.newLineAtOffset(xStart, 0);
        content.showText(line);
        content.newLineAtOffset(-xStart, -currentLeading);
    }

    public void addParagraph(String text) throws IOException {
        addParagraph(text, false);
    }

    public void addParagraph(String text, boolean justify) throws IOException {
        List<String> lines = parseLines(text, pageWidth);
        for (String line: lines) {
            float charSpacing = 0;
            if (justify){
                if (line.length() > 1) {
                    float size = calculateWidth(line);
                    float free = pageWidth - size;
                    if (free > 0 && !lines.get(lines.size() - 1).equals(line)) {
                        charSpacing = free / (line.length() - 1);
                    }
                }
            }
            content.setCharacterSpacing(charSpacing);
            content.showText(line);
            content.newLine();
        }
        addBlankLine();
    }

    public void addBlankLine() throws IOException {
        addBlankSpace(1.0f);
    }

    public void addBlankSpace(float lines) throws IOException {
        content.newLineAtOffset(0.0f, -lines*currentLeading);
    }

    public void startTable(int[] columnWidths, boolean[] alignments) {
        tableColumnWidths = columnWidths;
        tableAlignments = alignments;
    }

    public void addTableHeader(String... values) throws IOException {
        if(tableColumnWidths.length < tableAlignments.length || tableAlignments.length != values.length)
            throw new IllegalArgumentException("Table not configured correctly for input columns.");

        int totalWidth = 0;
        for(int i = 0; i<values.length; i++) {
            addTableCell(tableColumnWidths[i], tableAlignments[i], values[i]);
            content.newLineAtOffset(tableColumnWidths[i] + 5, 0);
            totalWidth += tableColumnWidths[i] + 5;
        }

        content.newLineAtOffset(-totalWidth, -currentLeading);
//        content.endText();
//        content.moveTo(-totalWidth, -1.15f*currentFontSize);
//        content.lineTo(totalWidth, 0);
//        content.moveTo(-totalWidth, -.1f*currentFontSize);
//        content.beginText();
    }

    public void addTableRow(String... values) throws IOException {
        addTableRow(tableColumnWidths, values);
    }

    public void addTableRow(int[] tableColumnWidths, String... values) throws IOException {
        if(tableColumnWidths.length < tableAlignments.length || tableAlignments.length != values.length)
            throw new IllegalArgumentException("Table not configured correctly for input columns.");

        int totalWidth = 0;
        for(int i = 0; i<values.length; i++) {
            addTableCell(tableColumnWidths[i], tableAlignments[i], values[i]);
            content.newLineAtOffset(tableColumnWidths[i] + 5, 0);
            totalWidth += tableColumnWidths[i] + 5;
        }
        content.newLineAtOffset(-totalWidth, -currentLeading);
    }

    @Override
    public void close() throws IOException {
        document.close();
    }

    public void writeToStream(OutputStream stream) throws IOException {
        closeContent();
        document.save(stream);
    }

    private void closeContent() throws IOException {
        if (this.content != null)
            this.content.close();
        this.content = null;
    }

    // ----- Private -----
    private List<String> parseLines(String text, float width) throws IOException {
        List<String> lines = new ArrayList<>();
        int lastSpace = -1;
        while (text.length() > 0) {
            int spaceIndex = text.indexOf(' ', lastSpace + 1);
            if (spaceIndex < 0)
                spaceIndex = text.length();
            String subString = text.substring(0, spaceIndex);
            float size = currentFontSize * FONT.getStringWidth(subString) / 1000;
            if (size > width) {
                if (lastSpace < 0){
                    lastSpace = spaceIndex;
                }
                subString = text.substring(0, lastSpace);
                lines.add(subString);
                text = text.substring(lastSpace).trim();
                lastSpace = -1;
            } else if (spaceIndex == text.length()) {
                lines.add(text);
                text = "";
            } else {
                lastSpace = spaceIndex;
            }
        }
        return lines;
    }

    private float calculateWidth(String line) throws IOException {
        return FONT.getStringWidth(line) / 1000 * currentFontSize;
    }

    private void addTableCell(float width, boolean alignLeft, String text) throws IOException {
        List<String> lines = parseLines(text, width);
        int linesWritten = 0;
        for (String line: lines) {
            if(linesWritten++ > 0)
                content.newLine();

            float whitespace = width - calculateWidth(line);
            if(!alignLeft) content.newLineAtOffset(whitespace, 0);
            content.showText(line);
            if(!alignLeft) content.newLineAtOffset(-whitespace, 0);
        }
    }

}
