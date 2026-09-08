package com.leetcode.backend.service;
import com.leetcode.backend.dto.CertificateResponse;
import java.time.Instant;
import java.nio.file.*;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CertificatePdfServiceTest {
 @Test void threeTiersHaveTrustedSelectableTextAndRender()throws Exception{
  CertificatePdfService service=new CertificatePdfService("https://verdixa.example");
  for(int tier:new int[]{50,100,150}){
   String id=UUID.randomUUID().toString();var record=new CertificateResponse(id,"Alexandra María Chen",tier,Instant.parse("2026-09-07T00:00:00Z"),tier,id,1,"VERIFIED");
   byte[] bytes=service.generate(record);try(var pdf=Loader.loadPDF(bytes)){
    assertEquals(1,pdf.getNumberOfPages());String text=new PDFTextStripper().getText(pdf);
    assertTrue(text.contains(record.recipientName()));assertTrue(text.contains(tier+" unique programming problems"));assertTrue(text.contains(id));assertTrue(text.contains("https://verdixa.example/certificate/"));
    Path output=Path.of("target","certificate-previews");Files.createDirectories(output);Files.write(output.resolve(tier+".pdf"),bytes);
    javax.imageio.ImageIO.write(new PDFRenderer(pdf).renderImageWithDPI(0,110),"png",output.resolve(tier+".png").toFile());
   }
  }
 }
 @Test void longNamesFitAndUnsupportedGlyphsDoNotBreakDownloads()throws Exception{
  var record=new CertificateResponse(UUID.randomUUID().toString(),"Very long recipient name ".repeat(4)+"🧑",150,Instant.now(),150,UUID.randomUUID().toString(),1,"VERIFIED");
  assertTrue(new CertificatePdfService("https://verdixa.example").generate(record).length>1000);
 }
}
