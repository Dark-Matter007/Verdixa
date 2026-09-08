package com.leetcode.backend.service;

import com.leetcode.backend.dto.CertificateResponse;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.awt.Color;
import java.io.*;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class CertificatePdfService {
    private final String publicBaseUrl;
    public CertificatePdfService(@Value("${verdixa.public-base-url:http://localhost:5173}") String publicBaseUrl) {
        this.publicBaseUrl=publicBaseUrl.replaceAll("/+$", "");
    }
    public byte[] generate(CertificateResponse certificate) throws IOException {
        try(PDDocument document=new PDDocument();ByteArrayOutputStream output=new ByteArrayOutputStream()) {
            PDPage page=new PDPage(new PDRectangle(842,595));document.addPage(page);
            PDFont font=PDType0Font.load(document,new org.springframework.core.io.ClassPathResource("fonts/DejaVuSans.ttf").getInputStream());
            Color accent=switch(certificate.milestone()){case 100->new Color(66,87,121);case 150->new Color(143,109,41);default->new Color(38,109,87);};
            try(PDPageContentStream canvas=new PDPageContentStream(document,page)) {
                canvas.setNonStrokingColor(new Color(250,249,245));canvas.addRect(0,0,842,595);canvas.fill();
                canvas.setStrokingColor(accent);canvas.setLineWidth(2);canvas.addRect(28,28,786,539);canvas.stroke();
                if(certificate.milestone()>=100){canvas.setLineWidth(.5f);canvas.addRect(35,35,772,525);canvas.stroke();}
                canvas.setNonStrokingColor(accent);canvas.addRect(390,525,62,4);canvas.fill();
                text(canvas,font,"VERDIXA",22,484,accent);
                text(canvas,font,"CERTIFICATE OF ACHIEVEMENT",14,437,accent);
                text(canvas,font,"Presented to",12,393,Color.DARK_GRAY);
                text(canvas,font,certificate.recipientName(),32,345,new Color(27,35,43));
                String tier=switch(certificate.milestone()){case 100->"DISTINCTION";case 150->"EXCELLENCE";default->"FOUNDATION";};
                text(canvas,font,certificate.milestone()+" PROBLEM MILESTONE  /  "+tier,17,287,accent);
                text(canvas,font,"Successfully solved "+certificate.milestone()+" unique programming problems on Verdixa.",13,248,Color.DARK_GRAY);
                text(canvas,font,"Awarded for demonstrated problem solving and consistent practice.",11,224,Color.DARK_GRAY);
                text(canvas,font,"Issued "+DateTimeFormatter.ofPattern("MMMM d, uuuu",java.util.Locale.ENGLISH).withZone(ZoneOffset.UTC).format(certificate.issuedAt()),11,173,Color.DARK_GRAY);
                text(canvas,font,"Certificate ID: "+certificate.publicCertificateId(),9,139,Color.DARK_GRAY);
                text(canvas,font,"Verify: "+publicBaseUrl+"/certificate/"+certificate.publicCertificateId(),9,115,Color.DARK_GRAY);
                text(canvas,font,"Verification code: "+certificate.verificationCode()+"  |  Version "+certificate.certificateVersion(),8,91,Color.DARK_GRAY);
                text(canvas,font,"VERIFIED AGAINST VERDIXA'S OFFICIAL SUBMISSION RECORDS",8,61,accent);
            }
            document.getDocumentInformation().setTitle("Verdixa "+certificate.milestone()+" Problem Milestone Certificate");
            document.save(output);return output.toByteArray();
        }
    }
    private void text(PDPageContentStream canvas,PDFont font,String text,float size,float y,Color color)throws IOException {
        // Preserve supported Unicode. Unsupported glyphs are replaced, never crash downloads.
        StringBuilder safe=new StringBuilder();
        text.codePoints().forEach(cp->{String ch=new String(Character.toChars(cp));try{font.encode(ch);safe.append(ch);}catch(Exception e){safe.append('?');}});
        String value=safe.toString();float width=font.getStringWidth(value)/1000*size;
        if(width>718){size*=718/width;width=718;}
        canvas.beginText();canvas.setFont(font,size);canvas.setNonStrokingColor(color);canvas.newLineAtOffset((842-width)/2,y);canvas.showText(value);canvas.endText();
    }
}
