package com.goldrenard.proximityfix;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * ������� ������������
 * @author Renard Gold (���� ������)
 */
public class BootReceiver extends BroadcastReceiver {   
    @Override
    public void onReceive(Context context, Intent intent) {
    	context.startService(new Intent(context, ProximityService.class));
	}
}