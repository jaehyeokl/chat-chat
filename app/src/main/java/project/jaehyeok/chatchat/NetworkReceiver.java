package project.jaehyeok.chatchat;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.widget.Toast;

// 어플리케이션 이용 중 네트워크 연결 상태가 변할때 실행된다
public class NetworkReceiver extends BroadcastReceiver {

    private Dialog dialog;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            // getNetworkInfo deprecated as of API 23
            NetworkInfo stateWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo stateMobile = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            Network activeNetwork = connectivityManager.getActiveNetwork();
            // wifi 와 mobile 둘다 연결되어 있지 않을때 activeNetwork == null
            if (activeNetwork == null) {
                //Log.d("NetworkReceiver", "네트워크 연결되지 않음");
                Toast.makeText(context, "네트워크와의 연결이 끊어졌습니다", Toast.LENGTH_SHORT).show();
                // 화면에 로딩이미지를 보여주도록 다이얼로그 설정
                loadingNetwork(context);
            } else {
                //Log.d("NetworkReceiver", "네트워크 연결");
                if (dialog != null) {
                    // 네트워크에 다시 연결되었을때 로딩 다이얼로그를 종료할 수 있도록 하기 위해서(dismiss)
                    // 다이얼로그를 전역변수르 설정한다
                    dialog.dismiss();
                    Toast.makeText(context, "네트워크 연결", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

        // 네트워크 상태값 받아오기
//        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
//            NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
//            NetworkInfo.DetailedState state = info.getDetailedState();
//
//            if (state == NetworkInfo.DetailedState.CONNECTED) {
//                // 네트워크가 연결 상태이면
//                Toast.makeText(context, "네트워크 연결", Toast.LENGTH_SHORT).show();
//            } else if (state == NetworkInfo.DetailedState.DISCONNECTED) {
//                // 네트워크가 연결 해제이면
//                Toast.makeText(context, "네트워크 연결 해제", Toast.LENGTH_SHORT).show();
//            }
//        }

    // 네트워크가 연결되기 전까지 화면에 보여질 로딩화면
    public void loadingNetwork(Context context) {
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.loading_network_dialog);
        //다이얼로그의 배경을 투명하게
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        // 다이얼로그 외부 터치 시 종료되지 않도록 설정
        dialog.setCancelable(false);

        // 다이얼로그 노출
        dialog.show();
    }
}
