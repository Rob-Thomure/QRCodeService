package com.example.qrcodeservice;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@RestController
public class TaskController {

    @GetMapping("/api/health")
    public int returnOne() {
        return 1;
    }

    boolean sizeLessThan150OrGreaterThan350(int size) {
        return size < 150 || size > 350;
    }

    ResponseEntity<?> invalidSizeResponse = ResponseEntity.badRequest()
            .body(new ErrorMessage("Image size must be between 150 and 350 pixels"));
    ResponseEntity<?> invalidTypeResponse = ResponseEntity.badRequest()
            .body(new ErrorMessage("Only png, jpeg and gif image types are supported"));
    ResponseEntity<?> invalidContentsResponse = ResponseEntity.badRequest()
            .body(new ErrorMessage("Contents cannot be null or blank"));
    ResponseEntity<?> invalidCorrectionResponse = ResponseEntity.badRequest()
            .body(new ErrorMessage("Permitted error correction levels are L, M, Q, H"));

    @GetMapping(path = "/api/qrcode")
    public ResponseEntity<?> getImage(@RequestParam String contents,
                                      @RequestParam(defaultValue = "250") int size,
                                      @RequestParam(defaultValue = "png") String type,
                                      @RequestParam(defaultValue = "L") String correction) {
        ResponseEntity<?> response;

        if (contents.isEmpty() || contents.isBlank()) {
            response = invalidContentsResponse;
        }
        else if (sizeLessThan150OrGreaterThan350(size)) {
            response = invalidSizeResponse;
        }
        else if (isInvalidCorrection(correction)) {
            response = invalidCorrectionResponse;
        }
        else if (isInvalidMediaType(type)) {
            response = invalidTypeResponse;
        } else {
            Optional<MediaType> mediaType = getMediaType(type);
            BufferedImage bufferedImage = getQRCodeBufferedImage(contents, size, correction);
            byte[] imageByteArray = getImageByteArray(bufferedImage, type);
            response = ResponseEntity.ok().contentType(mediaType.get()).body(imageByteArray);
        }
        return response;
    }

    private byte[] getImageByteArray(BufferedImage bufferedImage, String type) {
        try (var baos = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImage, type, baos);
            return baos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    private Optional<MediaType> getMediaType(String type) {
        return switch (type.toLowerCase()) {
            case "png" -> Optional.of(MediaType.IMAGE_PNG) ;
            case "jpeg" -> Optional.of(MediaType.IMAGE_JPEG);
            case "gif" -> Optional.of(MediaType.IMAGE_GIF);
            default -> Optional.empty();
        };
    }

    private BufferedImage getQRCodeBufferedImage(String data, int size, String correction) {
        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, ?> hints = Map.of(EncodeHintType.ERROR_CORRECTION,
                ErrorCorrectionLevel.valueOf(correction));
        try {
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size, hints);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        } catch (WriterException e) {
            System.out.println("Failed QR code writer");
            return null;
        }
    }

    private boolean isInvalidMediaType(String type) {
        return !type.matches("png|jpeg|gif");
    }

    private boolean isInvalidCorrection(String correction) {
        return !correction.matches("[LMQH]");
    }



}
