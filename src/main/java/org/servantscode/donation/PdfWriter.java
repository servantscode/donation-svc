package org.servantscode.donation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PdfWriter implements Closeable {
    private static final Logger LOG = LogManager.getLogger(PdfWriter.class);

    public enum Alignment { LEFT, CENTER, RIGHT, JUSTIFIED };
    public enum TextDecoration { UNDERLINE, DOUBLE_OVERLINE };

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

    private Alignment currentAlignment = Alignment.LEFT;
    private Set<TextDecoration> currentDecorations = new HashSet();

    private int[] columnWidths = new int[0];
    private Alignment[] columnAlignments = new Alignment[0];

    private float currentX = 0f;
    private float currentY = 0f;

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

    public void setAlignment(Alignment alignment) {
        this.currentAlignment = alignment;
    }

    public void addDecoration(TextDecoration decor) {
        currentDecorations.add(decor);
    }

    public void removeDecoration(TextDecoration decor) {
        currentDecorations.remove(decor);
    }
    public void addLine(String text) throws IOException {
        alignText(text, pageWidth);
        content.showText(text);
        newLine();
    }

    private void addText(String text, float width, Alignment alignment) throws IOException {
        float startX = currentX;
        alignText(text, width, alignment);
        float textStart = currentX;
        content.showText(text);
        moveRight(startX + width - currentX);

        if(!currentDecorations.isEmpty()) {
            endText();

            float textEnd = textStart + calculateWidth(text);
            for(TextDecoration decor: currentDecorations) {
                switch (decor) {
                    case UNDERLINE:
                        float underlineY = currentY - .2f * currentLeading;
                        drawLine(textStart, underlineY, textEnd, underlineY);
                        break;
                    case DOUBLE_OVERLINE:
                        float overlineOne = currentY + 1f * currentFontSize;
                        float overlineTwo = currentY + 1.2f * currentFontSize;
                        drawLine(textStart, overlineOne, textEnd, overlineOne);
                        drawLine(textStart, overlineTwo, textEnd, overlineTwo);
                        break;
                }
            }
            beginText();
        }
    }

    private void drawLine(float startX, float startY, float endX, float endY) throws IOException {
        content.moveTo(startX, startY);
        content.lineTo(endX, endY);
        content.stroke();
    }

    private void newLine() throws IOException {
        move(MARGIN_X - currentX, -currentLeading);
    }

    public void addParagraph(String text) throws IOException {
        List<String> lines = parseLines(text, pageWidth);
        for (String line: lines) {
            float charSpacing = 0;
            if (currentAlignment == Alignment.JUSTIFIED){
                if (line.length() > 1) {
                    float size = calculateWidth(line);
                    float free = pageWidth - size;
                    if (free > 0 && !lines.get(lines.size() - 1).equals(line)) {
                        charSpacing = free / (line.length() - 1);
                    }
                }
            }
            content.setCharacterSpacing(charSpacing);

            addLine(line);
        }
        addBlankLine();
    }

    public void addBlankLine() throws IOException {
        addBlankSpace(1.0f);
    }

    public void addBlankSpace(float lines) throws IOException {
        moveDown(-lines*currentLeading);
    }

    public void startTable(int[] columnWidths, Alignment[] alignments) {
        this.columnWidths = columnWidths;
        columnAlignments = alignments;
    }

    public void addTableHeader(String... values) throws IOException {
        if(columnWidths.length < columnAlignments.length || columnAlignments.length != values.length)
            throw new IllegalArgumentException("Table not configured correctly for input columns.");

        addDecoration(TextDecoration.UNDERLINE);
        for(int i = 0; i<values.length; i++) {
            addTableCell(values[i], columnWidths[i], columnAlignments[i]);
            moveRight(10); //boundary spacing
        }
        removeDecoration(TextDecoration.UNDERLINE);

        newLine();
    }

    public void addTableRow(String... values) throws IOException {
        addTableRow(columnWidths, values);
    }

    public void addTableRow(int[] tableColumnWidths, String... values) throws IOException {
        if(tableColumnWidths.length < columnAlignments.length || columnAlignments.length != values.length)
            throw new IllegalArgumentException("Table not configured correctly for input columns.");

        for(int i = 0; i<values.length; i++) {
            addTableCell(values[i], tableColumnWidths[i], columnAlignments[i]);
            moveRight(10); //cell border spacing
        }
        newLine();
    }

    @Override
    public void close() throws IOException {
        document.close();
    }

    public void writeToStream(OutputStream stream) throws IOException {
        closeContent();
        document.save(stream);
    }

    // ----- Private -----
    private void closeContent() throws IOException {
        if (this.content != null)
            this.content.close();
        this.content = null;
    }

    private List<String> parseLines(String text, float width) throws IOException {
        List<String> lines = new ArrayList<>();
        int lastSpace = -1;
        while (text.length() > 0) {
            int spaceIndex = text.indexOf(' ', lastSpace + 1);
            if (spaceIndex < 0)
                spaceIndex = text.length();
            String subString = text.substring(0, spaceIndex);
            float size = calculateWidth(subString);
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

    private void addTableCell(String text, float width, Alignment alignment) throws IOException {
        if(text.isEmpty())
            moveRight(width);

        List<String> lines = parseLines(text, width);
        float startX = currentX;
        boolean first = true;
        for (String line: lines) {
            if(first)
                first = false;
            else
                move(startX - currentX, -currentLeading);

            addText(line, width, alignment);
        }
    }

    private void alignText(String text, float width) throws IOException {
        alignText(text, width, currentAlignment);
    }

    private void alignText(String text, float width, Alignment alignment) throws IOException {
        switch (alignment) {
            case CENTER:
                moveRight((width - calculateWidth(text)) / 2);
                return;
            case RIGHT:
                moveRight(width - calculateWidth(text));
                return;
            default:
        }
    }

    // Cursor Movement
    private void moveRight(float deltaX) throws IOException {
        currentX += deltaX;
        if(currentX > (pageWidth + MARGIN_X))
            LOG.warn("Writing text beyond page width boundary");

        content.newLineAtOffset(deltaX, 0);
    }

    private void moveDown(float deltaY) throws IOException {
        currentY += deltaY;
        if(currentY < MARGIN_Y) {
            endText();
            newPage();
            beginText();
        } else {
            content.newLineAtOffset(0, deltaY);
        }
    }

    private void move(float deltaX, float deltaY) throws IOException {
        currentX += deltaX;
        currentY += deltaY;
        if(currentY < MARGIN_Y) {
            endText();
            newPage();
            beginText();
        } else {
            content.newLineAtOffset(deltaX, deltaY);
        }
    }
}
