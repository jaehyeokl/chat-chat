package project.jaehyeok.chatchat;

import android.app.Activity;
import android.widget.Toast;

// 백버튼을 2번 눌럿을때 앱 종료되도록 한다
public class BackPressHandler {

    private long backKeyPressedTime = 0; // 마지막으로 뒤로가기 버튼을 눌렀던 시간 저장
    private Toast toast; // 첫 번째 뒤로가기 버튼을 누를때 표시
    private Activity activity; // 종료시킬 Activity

    // 종료시킬 액티비티
    public BackPressHandler(Activity activity) {
        this.activity = activity;
    }

    // default 백버튼을 2초이내 한번 더 누르면 종료
    public void onBackPressed() {
        if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
            backKeyPressedTime = System.currentTimeMillis();
            showGuide();
            return;
        }
        if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
            activity.finish();
            toast.cancel();
        }
    }

    // 토스트 메세지 사용자 지정
    public void onBackPressed(String msg) {
        if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
            backKeyPressedTime = System.currentTimeMillis();
            showGuide(msg);
            return;
        }
        if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
            activity.finish();
            toast.cancel();
        }
    }

    // 두번째 백버튼 입력 간격 지정 (단위 milliseconds)
    public void onBackPressed(int time) {
        if (System.currentTimeMillis() > backKeyPressedTime + time) {
            backKeyPressedTime = System.currentTimeMillis();
            showGuide();
            return;
        }
        if (System.currentTimeMillis() <= backKeyPressedTime + time) {
            activity.finish();
            toast.cancel();
        }
    }

    // 토스트메세지, 버튼 입력간격 지정
    public void onBackPressed(String msg, int time) {
        if (System.currentTimeMillis() > backKeyPressedTime + time) {
            backKeyPressedTime = System.currentTimeMillis();
            showGuide(msg);
            return;
        }
        if (System.currentTimeMillis() <= backKeyPressedTime + time) {
            activity.finish();
            toast.cancel();
        }
    }


    // 기본 메세지
    private void showGuide() {
        toast = Toast.makeText(activity, "\'뒤로\' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT);
        toast.show();
    }

    // 토스트 메세지 사용자 지정
    private void showGuide(String msg) {
        toast = Toast.makeText(activity, msg, Toast.LENGTH_SHORT);
        toast.show();
    }
}
