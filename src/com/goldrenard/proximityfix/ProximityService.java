package com.goldrenard.proximityfix;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

/**
 * Служба слежения за состояниями вызова и датчика приближения
 * @author Renard Gold (Илья Егоров)
 */
public class ProximityService extends Service {

	private static final String LOG_TAG = "ProximityService";
	private PowerManager mPowerManager;
	private TelephonyManager mTelephonyManager;
	private SensorManager mSensorManager; 
	private Sensor mSensor;
    private WakeLock mWakeLock;
	
    private int PROXIMITY_SCREEN_OFF_WAKE_LOCK = PowerManager.PARTIAL_WAKE_LOCK;
	
    /**
     * Листенер состояния датчика приближения
     */
	private SensorEventListener mProximityListener = new SensorEventListener() {
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) { }
		@Override
		public void onSensorChanged(SensorEvent event) { doBlock(event.values[0] == 0); }
	};
	
	/**
	 * Листенер состояния вызова
	 */
	private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        public void onCallStateChanged(int state, String incomingNumber) {
        	switch (state) {
        		case TelephonyManager.CALL_STATE_IDLE:		// окончание вызова
        			mSensorManager.unregisterListener(mProximityListener);
        			doBlock(false);
        			break;
             	case TelephonyManager.CALL_STATE_OFFHOOK:	// активное состояние вызова
             		mSensorManager.registerListener(mProximityListener, mSensor, SensorManager.SENSOR_DELAY_FASTEST);
             		break;
             }
         }
	};
	
	public void onCreate() {
		super.onCreate();
		mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		mTelephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		mPowerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
		
        try { PROXIMITY_SCREEN_OFF_WAKE_LOCK = PowerManager.class.getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null); } 
        catch (Throwable ignored) { }
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        return START_STICKY;
	}
	
	public void onDestroy() {
		mSensorManager.unregisterListener(mProximityListener);
		mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
		releaseWakeLock();
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	/**
	 * Получение WakeLock
	 * @param isBlocked Нужно ли заблокировать
	 */
	public void doBlock(boolean isBlocked) {
		if (isBlocked) {
			acquireWakeLock(false);
		} else {
			acquireWakeLock(true);		// отпускаем блок и форсим включение экрана
			releaseWakeLock();
		}
	}
	
    /* ==============================================================================
     * 								WAKE LOCK SECTION
     * ============================================================================== */

    /**
     * Получение блока
     * @param isWake Нужно ли разбудить девайс
     */
	private void acquireWakeLock(boolean isWake) {
    	releaseWakeLock();	// на всякий случай отпускаем предыдущий блок если есть
        if (isWake) {
        	mWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, LOG_TAG);
        } else {
        	mWakeLock = mPowerManager.newWakeLock(PROXIMITY_SCREEN_OFF_WAKE_LOCK, LOG_TAG);
        }
        mWakeLock.setReferenceCounted(false);
        mWakeLock.acquire();
    }

    /**
     * Отпускаем блок
     */
    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        mWakeLock = null;
    }
}
