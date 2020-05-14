package cn.bingerz.bledemo;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.core.AllOf.allOf;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest {
    private static final String TAG = MainActivityTest.class.getSimpleName();

    private int mServiceAdapterItemCount = 0;
    private int mCharacteristicAdapterItemCount = 0;
    private int mPropertyAdapterItemCount = 0;

    /**
     * {@link ActivityTestRule} is a JUnit {@link Rule @Rule} to launch your activity under test.
     * <p>
     * <p>
     * Rules are interceptors which are executed for each test method and are important building
     * blocks of Junit tests.
     */
    @Rule
    public ActivityTestRule<MainActivity> mMainActivityTestRule =
            new ActivityTestRule<>(MainActivity.class);

    @Before
    public void initIdlingResource() {
        registerIdlingResource();
    }

    public class ServiceItemNotNull implements ViewAssertion {

        public ServiceItemNotNull() {
        }

        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }

            RecyclerView recyclerView = (RecyclerView) view;
            RecyclerView.Adapter adapter = recyclerView.getAdapter();
//            Assert.assertTrue(adapter.getItemCount() > 0);
            mServiceAdapterItemCount = adapter.getItemCount();
        }
    }

    public class CharacteristicItemNotNull implements ViewAssertion {

        public CharacteristicItemNotNull() {
        }

        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
            RecyclerView recyclerView = (RecyclerView) view;
            RecyclerView.Adapter adapter = recyclerView.getAdapter();
//            Assert.assertTrue(adapter.getItemCount() > 0);
            mCharacteristicAdapterItemCount = adapter.getItemCount();
        }
    }


    public class PropertyItemNotNull implements ViewAssertion {

        public PropertyItemNotNull() {
        }

        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
            RecyclerView recyclerView = (RecyclerView) view;
            RecyclerView.Adapter adapter = recyclerView.getAdapter();
//            Assert.assertTrue(adapter.getItemCount() > 0);
            mPropertyAdapterItemCount = adapter.getItemCount();
        }
    }

    public static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specified id.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                View v = view.findViewById(id);
                v.performClick();
            }
        };
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private int clickScanButton_ReturnItemCount() {
        onView(withId(R.id.btn_scan)).perform(click());
        RecyclerView recyclerView = mMainActivityTestRule.getActivity().findViewById(R.id.rv_list);
        return recyclerView.getAdapter().getItemCount();
    }

    @Test
    public void clickScan_ScrollToEnd() {
        int itemCount = clickScanButton_ReturnItemCount();
        Assert.assertTrue(itemCount > 0);
        onView(withId(R.id.rv_list)).perform(RecyclerViewActions.scrollToPosition(itemCount - 1));
        sleep(5000);
    }

    @Test
    public void clickScan_Loop() {
        int scanLoopCount = 50;
        for (int i = 0; i < scanLoopCount; i++) {
            int itemCount = clickScanButton_ReturnItemCount();
            Assert.assertTrue(itemCount > 0);
            sleep(5000);
        }
    }

    private void clickConnectDeviceByIndexWhenScannedListNotNull(int index) {
        onView(ViewMatchers.withId(R.id.rv_list)).perform(
                RecyclerViewActions.actionOnItemAtPosition(index, clickChildViewWithId(R.id.btn_connect)));

        //触发连接后，idlingResource不能马上increment，添加延时确保increment执行。
        sleep(500);
    }

    private void clickDeviceDetailByIndex(int index) {
        onView(ViewMatchers.withId(R.id.rv_list)).check(matches(isDisplayed()));

        onView(ViewMatchers.withId(R.id.rv_list)).perform(RecyclerViewActions.scrollToPosition(index));

        onView(ViewMatchers.withId(R.id.rv_list)).perform(
                RecyclerViewActions.actionOnItemAtPosition(index, clickChildViewWithId(R.id.btn_detail)));
    }

    private void clickDisconnectDeviceByIndex(int index) {
        onView(ViewMatchers.withId(R.id.rv_list)).check(matches(isDisplayed()));

        onView(ViewMatchers.withId(R.id.rv_list)).perform(
                RecyclerViewActions.actionOnItemAtPosition(index, clickChildViewWithId(R.id.btn_disconnect)));

    }

    @Test
    public void clickConnect_IteratorAll_Disconnect_Loop() {
        int itemCount = clickScanButton_ReturnItemCount();
        Assert.assertTrue(itemCount > 0);

        Log.d(TAG,"Item count is " + itemCount);

        for (int i = 0; i < itemCount; i++) {
            Log.d(TAG, "Connect index " + i);
            clickConnectDeviceByIndexWhenScannedListNotNull(i);

            clickDeviceDetailByIndex(itemCount - 1);

            onView(withId(R.id.tv_service_list_mac)).check(matches(isDisplayed()));
            iteratorServiceCharacteristicProperty();

            Espresso.pressBack();

            clickDisconnectDeviceByIndex(itemCount - 1);

            itemCount--;
            // Waiting for the device to be disconnected
            sleep(2000);
        }
    }

    @Test
    public void clickConnect_Disconnect_Loop() {
        int itemCount = clickScanButton_ReturnItemCount();
        Assert.assertTrue(itemCount > 0);

        Log.d("Item count is ", String.valueOf(itemCount));

        for (int i = 0; i < itemCount; i++) {
            Log.d(TAG, "Connect index " + i);
            clickConnectDeviceByIndexWhenScannedListNotNull(i);

            clickDeviceDetailByIndex(itemCount - 1);

            onView(withId(R.id.tv_service_list_mac)).check(matches(isDisplayed()));

            Espresso.pressBack();

            clickDisconnectDeviceByIndex(itemCount - 1);

            itemCount--;

            sleep(2000);
        }
    }

    private int checkServiceListNotNull_GetCount() {
        onView(ViewMatchers.withId(R.id.rv_service_list)).check(matches(isDisplayed()));
        onView(withId(R.id.rv_service_list)).check(new ServiceItemNotNull());
        return mServiceAdapterItemCount;
    }

    private void clickService(int index) {
        onView(ViewMatchers.withId(R.id.rv_service_list)).perform(
                RecyclerViewActions.actionOnItemAtPosition(index, click()));
    }

    private int checkCharacteristicListNotNull_GetCount() {
//        onView(ViewMatchers.withId(R.id.rv_characteristic_list)).check(matches(isDisplayed()));
        onView(withId(R.id.rv_characteristic_list)).check(new CharacteristicItemNotNull());
        return mCharacteristicAdapterItemCount;
    }

    private void clickCharacteristic(int index) {
        onView(ViewMatchers.withId(R.id.rv_characteristic_list)).perform(
                RecyclerViewActions.actionOnItemAtPosition(index, click()));
    }

    private int checkPropertyListNotNull_GetCount() {
        onView(ViewMatchers.withId(R.id.rv_property_list)).check(matches(isDisplayed()));
        onView(withId(R.id.rv_property_list)).check(new PropertyItemNotNull());
        return mPropertyAdapterItemCount;
    }

    private void clickProperty(int index) {
        onView(ViewMatchers.withId(R.id.rv_property_list)).perform(
                RecyclerViewActions.actionOnItemAtPosition(index, click()));
    }

    private void iteratorServiceCharacteristicProperty() {
        int serviceItemCount = checkServiceListNotNull_GetCount();
        for (int i = 0; i < serviceItemCount; i++) {
            clickService(i);
            int characteristicItemCount = checkCharacteristicListNotNull_GetCount();
            for (int j = 0; j < characteristicItemCount; j++) {
                clickCharacteristic(j);
                int propertyItemCount = checkPropertyListNotNull_GetCount();
                for (int k = 0; k < propertyItemCount; k++) {
                    clickProperty(k);
                    onView(allOf(withId(R.id.btn), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).perform(click());
                    sleep(1000);
                    Espresso.pressBack();
                }
                Espresso.pressBack();
            }
            Espresso.pressBack();
        }
    }

    @Test
    public void clickConnectFirst_IteratorAll() {
        int itemCount = clickScanButton_ReturnItemCount();
        Assert.assertTrue(itemCount > 0);
        clickConnectDeviceByIndexWhenScannedListNotNull(0);
        clickDeviceDetailByIndex(itemCount - 1);
        iteratorServiceCharacteristicProperty();
        Espresso.pressBack();
        clickDisconnectDeviceByIndex(itemCount - 1);
    }

    private boolean iteratorExecuteWriteShutdown() {
        int serviceItemCount = checkServiceListNotNull_GetCount();
        if (serviceItemCount < 6 || serviceItemCount > 7) {
            Log.e(TAG, "service item count abnormal.");
            return false;
        }
        clickService(serviceItemCount - 1);

        int characteristicItemCount = checkCharacteristicListNotNull_GetCount();
        if (characteristicItemCount > 1) {
            Log.e(TAG, "characteristic item count abnormal.");
            Espresso.pressBack();
            return false;
        }
        clickCharacteristic(0);

        int propertyItemCount = checkPropertyListNotNull_GetCount();
        if (propertyItemCount != 3) {
            Log.e(TAG, "property item count abnormal.");
            Espresso.pressBack();
            Espresso.pressBack();
            return false;
        }

        clickProperty(1);

        onView(allOf(withId(R.id.et), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).perform(replaceText("06"));

        sleep(500);

        onView(allOf(withId(R.id.btn), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).perform(click());
        return true;
    }

    @Test
    public void clickConnect_Shutdown_IteratorAll() {
        int itemCount = clickScanButton_ReturnItemCount();
        Assert.assertTrue(itemCount > 0);
        for (int i = itemCount; i > 0; i--) {
            clickConnectDeviceByIndexWhenScannedListNotNull(0);

            clickDeviceDetailByIndex(itemCount - 1);

            boolean result = iteratorExecuteWriteShutdown();
            if (result) {
                sleep(5000);
            } else {
                Espresso.pressBack();
                clickDisconnectDeviceByIndex(itemCount - 1);
            }
            itemCount--;
        }
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    public void unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(
                mMainActivityTestRule.getActivity().getCountingIdlingResource());
    }

    /**
     * Convenience method to register an IdlingResources with Espresso. IdlingResource resource is
     * a great way to tell Espresso when your app is in an idle state. This helps Espresso to
     * synchronize your test actions, which makes tests significantly more reliable.
     */
    private void registerIdlingResource() {
        IdlingRegistry.getInstance().register(
                mMainActivityTestRule.getActivity().getCountingIdlingResource());
    }
}
