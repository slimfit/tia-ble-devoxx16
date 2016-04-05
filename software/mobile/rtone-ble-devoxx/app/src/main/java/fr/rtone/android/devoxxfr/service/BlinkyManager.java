/**  * Created by Alexis DUQUE - alexisd61@gmail.com.  * Date : 05/04/16.  */

package fr.rtone.android.devoxxfr.service;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import fr.rtone.android.devoxxfr.profile.BleManager;

public class BlinkyManager extends BleManager<BlinkyManagerCallbacks> {

	private final static UUID BATTERY_SERVICE = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
	private final static UUID BATTERY_LEVEL_CHARACTERISTIC = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");

	private final static UUID HRM_SERVICE = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
	private final static UUID HRM_CHARACTERISTIC = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");

	private final static String TAG = "BlinkyManager";

	private BluetoothGattCharacteristic mBatteryCharacteristic, mHrmCharacteristic;

	public BlinkyManager(final Context context) {
		super(context);
	}

	@Override
	protected BleManagerGattCallback getGattCallback() {
		return mGattCallback;
	}

	/**
	 * BluetoothGatt callbacks for connection/disconnection, service discovery, receiving indication, etc
	 */
	private final BleManagerGattCallback mGattCallback = new BleManagerGattCallback() {

		@Override
		protected Queue<Request> initGatt(final BluetoothGatt gatt) {
			final LinkedList<Request> requests = new LinkedList<>();
			requests.push(Request.newEnableNotificationsRequest(mBatteryCharacteristic));
			requests.push(Request.newEnableNotificationsRequest(mHrmCharacteristic));
			return requests;
		}

		@Override
		public boolean isRequiredServiceSupported(final BluetoothGatt gatt) {
			final BluetoothGattService service = gatt.getService(BATTERY_SERVICE);
			if (service != null) {
				mBatteryCharacteristic = service.getCharacteristic(BATTERY_LEVEL_CHARACTERISTIC);
				Log.d(TAG, "Battery char. found");
			} else {
				Log.w(TAG, "Battery service not found");
			}
			final BluetoothGattService hrmService = gatt.getService(HRM_SERVICE);
			if (hrmService != null) {
				mHrmCharacteristic = hrmService.getCharacteristic(HRM_CHARACTERISTIC);
			} else {
				Log.w(TAG, "HRM service not found");
			}

			boolean writeRequest = false;
			if (mBatteryCharacteristic != null) {
				final int rxProperties = mBatteryCharacteristic.getProperties();
				writeRequest = (rxProperties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0;
			} else {
				Log.w(TAG, "Missing Write permission on Battery char.");
			}

			return mHrmCharacteristic != null && mBatteryCharacteristic != null && writeRequest;
		}

		@Override
		protected void onDeviceDisconnected() {
			mBatteryCharacteristic = null;
			mHrmCharacteristic = null;
		}

		@Override
		public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
			mCallbacks.onDataSent(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) == 0x01);
			Log.i(TAG, "char. writed :" + characteristic.getUuid().toString());
		}

		@Override
		public void onCharacteristicNotified(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
			int data = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
			Log.i(TAG, characteristic.getUuid().toString() + " notif. received: 0x" + data);
			mCallbacks.onDataReceived(data == 0x01);
		}
	};

	public void send(final boolean onOff) {
		// Are we connected?
		if (mBatteryCharacteristic == null)
			return;

		byte [] command;
		if (onOff){
			command = new byte [] {1};
		} else {
			command = new byte [] {0};
		}
		mBatteryCharacteristic.setValue(command);
		writeCharacteristic(mBatteryCharacteristic);
	}
}
