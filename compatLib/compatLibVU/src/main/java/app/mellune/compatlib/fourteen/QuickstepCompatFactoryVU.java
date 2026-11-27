package app.mellune.compatlib.fourteen;

import android.window.RemoteTransition;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import app.mellune.compatlib.ActivityManagerCompat;
import app.mellune.compatlib.ActivityOptionsCompat;
import app.mellune.compatlib.RemoteTransitionCompat;
import app.mellune.compatlib.thirteen.QuickstepCompatFactoryVT;

@RequiresApi(34)
public class QuickstepCompatFactoryVU extends QuickstepCompatFactoryVT {

    @NonNull
    @Override
    public ActivityManagerCompat getActivityManagerCompat() {
        return new ActivityManagerCompatVU();
    }

    @NonNull
    @Override
    public ActivityOptionsCompat getActivityOptionsCompat() {
        return new ActivityOptionsCompatVU();
    }

    @NonNull
    @Override
    public RemoteTransitionCompat getRemoteTransitionCompat() {
        return RemoteTransition::new;
    }
}
