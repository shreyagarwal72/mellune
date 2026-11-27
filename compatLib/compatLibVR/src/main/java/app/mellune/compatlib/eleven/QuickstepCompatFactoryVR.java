package app.mellune.compatlib.eleven;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import app.mellune.compatlib.ActivityManagerCompat;
import app.mellune.compatlib.ActivityOptionsCompat;
import app.mellune.compatlib.ten.QuickstepCompatFactoryVQ;

@RequiresApi(30)
public class QuickstepCompatFactoryVR extends QuickstepCompatFactoryVQ {

    @NonNull
    @Override
    public ActivityManagerCompat getActivityManagerCompat() {
        return new ActivityManagerCompatVR();
    }

    @NonNull
    @Override
    public ActivityOptionsCompat getActivityOptionsCompat() {
        return new ActivityOptionsCompatVR();
    }
}
