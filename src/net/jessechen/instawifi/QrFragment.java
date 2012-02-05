package net.jessechen.instawifi;

import net.jessechen.instawifi.util.WifiUtil;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QrFragment extends Fragment {
	private static final String TAG = QrFragment.class.getSimpleName();

	public static QrFragment getInstance() {
		QrFragment fragment = new QrFragment();

		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.qr_activity, container, false);
	
		ImageView img = (ImageView) view.findViewById(R.id.qr_code_image);
		Bitmap bitmap = generateQrImage("mynetwork", WifiUtil.WPA, "password");
		img.setImageBitmap(bitmap);

		return view;
	}

	private Bitmap generateQrImage(String string, String wPA, String string2) {
		QRCodeWriter writer = new QRCodeWriter();
		BitMatrix bm = null;
		try {
			bm = writer.encode("WIFI:T:WPA;S:HerwoWorld;P:cabdad1234;;",
					BarcodeFormat.QR_CODE, 250, 250);
		} catch (WriterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int width = bm.getWidth();
		int height = bm.getHeight();
		int[] pixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				pixels[offset + x] = bm.get(x, y) ? Color.BLACK : Color.WHITE;
			}
		}

		Bitmap bitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		
		return bitmap;
	}
}
