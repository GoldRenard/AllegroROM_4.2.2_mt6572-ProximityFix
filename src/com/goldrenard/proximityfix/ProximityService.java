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

@SuppressWarnings("deprecation")
public class ProximityService extends Service {

	private static final String LOG_TAG = "ProximityService";
	private PowerManager mPowerManager;
	private TelephonyManager mTelephonyManager;
	private SensorManager mSensorManager; 
	private Sensor mSensor;
    private WakeLock mWakeLock;
	
    private int PROXIMITY_SCREEN_OFF_WAKE_LOCK = PowerManager.PARTIAL_WAKE_LOCK;
	
    /**
     * Листенер датчика приближения
     */
	private SensorEventListener mProximityListener = new SensorEventListener() {
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) { }
		@Override
		public void onSensorChanged(SensorEvent event) { doBlock(event.values[0] == 0); }
	};
	
	/**
	 * Листенер действий телефона
	 */
	private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        public void onCallStateChanged(int state, String incomingNumber) {
        	switch (state) {
        		case TelephonyManager.CALL_STATE_IDLE:		// Звонок окончен или отсутствует
        			mSensorManager.unregisterListener(mProximityListener);
        			doBlock(false);
        			break;
             	case TelephonyManager.CALL_STATE_OFFHOOK:	// Начало звонка
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
	 * Получить блок
	 * @param isBlocked Нужно ли получить блок
	 */
	public void doBlock(boolean isBlocked) {
		if (isBlocked) {
			acquireWakeLock(false);
		} else {
			acquireWakeLock(true);		// форсируем просыпание
			releaseWakeLock();
		}
	}
	
    /* ==============================================================================
     * 								WAKE LOCK SECTION
     * ============================================================================== */

    /**
     * Получение блока
     * @param isWake Если True, то форсим просыпание девайса
     */
	private void acquireWakeLock(boolean isWake) {
    	releaseWakeLock();	// отпускаем старый блок на всякий случай
        if (isWake) {
        	mWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, LOG_TAG);
        } else {
        	mWakeLock = mPowerManager.newWakeLock(PROXIMITY_SCREEN_OFF_WAKE_LOCK, LOG_TAG);
        }
        mWakeLock.setReferenceCounted(false);
        mWakeLock.acquire();
    }

    /**
     * Отпустить блокировку
     */
    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        mWakeLock = null;
    }
}
