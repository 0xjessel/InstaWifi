package net.jessechen.instawifi.util;

import net.jessechen.instawifi.models.WifiModel;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QrUtil {
	public static String NOPASS = "nopass";

	public enum QrImageSize {
		SMALL, LARGE
	}

	public static Bitmap generateQrCode(WifiModel wm, QrImageSize size) {
		// padding around the edges
		final int MAGIC_NUMBER = (size.equals(QrImageSize.SMALL)) ? 30 : 60;
		// height and width of qr code
		final int DIMENSION = (size.equals(QrImageSize.SMALL)) ? 350 : 700;

		if (!WifiUtil.isValidWifiModel(wm)) {
			return null;
		}

		QRCodeWriter writer = new QRCodeWriter();
		BitMatrix bm = null;
		try {
			// a little hack for open network configurations s.t. barcode
			// scanner is happy
			String textProtocol = "";
			if (wm.getProtocol() == WifiUtil.OPEN) {
				textProtocol = NOPASS;
			} else {
				textProtocol = WifiUtil.protocolStrings[wm.getProtocol()];
			}

			bm = writer.encode(
					String.format(WifiUtil.QR_WIFI_URI_SCHEME, textProtocol,
							wm.getSSID(), wm.getPassword()),
					BarcodeFormat.QR_CODE, DIMENSION, DIMENSION);
		} catch (WriterException e) {
			e.printStackTrace();
		}

		int width = bm.getWidth() - (MAGIC_NUMBER * 2);
		int height = bm.getHeight() - (MAGIC_NUMBER * 2);
		int[] pixels = new int[width * height];
		for (int y = MAGIC_NUMBER; y < bm.getHeight() - MAGIC_NUMBER; y++) {
			int offset = (y - MAGIC_NUMBER) * width;
			for (int x = MAGIC_NUMBER; x < bm.getWidth() - MAGIC_NUMBER; x++) {
				pixels[offset + (x - MAGIC_NUMBER)] = bm.get(x, y) ? Color.BLACK
						: Color.WHITE;
			}
		}

		Bitmap bitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

		return bitmap;
	}
	
	public static String getQrFilename(String ssid) {
		if (ssid != null) {
			return "InstaWifi " + ssid + " QR" + ".jpg";
		} else {
			throw new NullPointerException("null ssid");
		}
	}
}
