package info.nightscout.androidaps.plugins.Treatments;

import com.squareup.otto.Subscribe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

import info.nightscout.androidaps.Config;
import info.nightscout.androidaps.Constants;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.Iob;
import info.nightscout.androidaps.data.MealData;
import info.nightscout.androidaps.db.Treatment;
import info.nightscout.androidaps.events.EventTreatmentChange;
import info.nightscout.androidaps.interfaces.PluginBase;
import info.nightscout.androidaps.interfaces.TreatmentsInterface;
import info.nightscout.androidaps.data.IobTotal;
import info.nightscout.androidaps.plugins.ConfigBuilder.ConfigBuilderPlugin;
import info.nightscout.client.data.NSProfile;
import info.nightscout.utils.DateUtil;
import info.nightscout.utils.SimpleResultCache;

/**
 * Created by mike on 05.08.2016.
 */
public class TreatmentsPlugin implements PluginBase, TreatmentsInterface {
    private static Logger log = LoggerFactory.getLogger(TreatmentsPlugin.class);

    public static long lastCalculationTimestamp = 0;
    public static IobTotal lastCalculation;

    public static List<Treatment> treatments;

    private static boolean fragmentEnabled = true;
    private static boolean fragmentVisible = true;

    SimpleResultCache<IobTotal> simpleResultCache = new SimpleResultCache<>();

    @Override
    public String getFragmentClass() {
        return TreatmentsFragment.class.getName();
    }

    @Override
    public String getName() {
        return MainApp.instance().getString(R.string.treatments);
    }

    @Override
    public String getNameShort() {
        String name = MainApp.sResources.getString(R.string.treatments_shortname);
        if (!name.trim().isEmpty()){
            //only if translation exists
            return name;
        }
        // use long name as fallback
        return getName();
    }

    @Override
    public boolean isEnabled(int type) {
        return type == TREATMENT && fragmentEnabled;
    }

    @Override
    public boolean isVisibleInTabs(int type) {
        return type == TREATMENT && fragmentVisible;
    }

    @Override
    public boolean canBeHidden(int type) {
        return true;
    }

    @Override
    public void setFragmentEnabled(int type, boolean fragmentEnabled) {
        if (type == TREATMENT) this.fragmentEnabled = fragmentEnabled;
    }

    @Override
    public void setFragmentVisible(int type, boolean fragmentVisible) {
        if (type == TREATMENT) this.fragmentVisible = fragmentVisible;
    }

    @Override
    public int getType() {
        return PluginBase.TREATMENT;
    }

    public TreatmentsPlugin() {
        MainApp.bus().register(this);
        initializeData();
    }

    public void initializeData() {
        double dia = 3;
        if (MainApp.getConfigBuilder().getActiveProfile() != null && MainApp.getConfigBuilder().getActiveProfile().getProfile() != null)
            dia = MainApp.getConfigBuilder().getActiveProfile().getProfile().getDia();
        long fromMills = (long) (new Date().getTime() - 60 * 60 * 1000L * (24 + dia));
        treatments = MainApp.getDbHelper().getTreatmentDataFromTime(fromMills, false);
    }

    /*
     * Recalculate IOB if value is older than 1 minute
     */
    public void updateTotalIOBIfNeeded() {
        if (lastCalculationTimestamp > new Date().getTime() - 60 * 1000)
            return;
        updateTotalIOB();
    }

    @Override
    public IobTotal getLastCalculation() {
        return lastCalculation;
    }

    @Override
    public IobTotal getCalculationToTime(long time) {
        IobTotal cachedValue = null;
        if (Config.useResultCaching) {
            cachedValue = simpleResultCache.get(time, MainApp.getDbHelper().latestTreatmentChange());
            if (cachedValue != null) {
                if (Config.logResultCache)
                    log.debug("Cache hit. time: " + time);
                return cachedValue;
            }
        }
        IobTotal total = new IobTotal(time);

        if (MainApp.getConfigBuilder() == null || ConfigBuilderPlugin.getActiveProfile() == null) // app not initialized yet
            return total;
        NSProfile profile = ConfigBuilderPlugin.getActiveProfile().getProfile();
        if (profile == null)
            return total;

        Double dia = profile.getDia();

        for (Integer pos = 0; pos < treatments.size(); pos++) {
            Treatment t = treatments.get(pos);
            if (t.created_at.getTime() > time) continue;
            Iob tIOB = t.iobCalc(time, dia);
            total.iob += tIOB.iobContrib;
            total.activity += tIOB.activityContrib;
            Iob bIOB = t.iobCalc(time, dia / Constants.BOLUSSNOOZE_DIA_ADVISOR);
            total.bolussnooze += bIOB.iobContrib;
        }
        if (Config.useResultCaching) {
            if (Config.logResultCache)
                log.debug("Cache miss. time: " + time);
            simpleResultCache.put(total, time, MainApp.getDbHelper().latestTreatmentChange());
        }
        return total;
    }

    @Override
    public void updateTotalIOB() {
        long time = DateUtil.floorToMinute(new Date().getTime());
        IobTotal total = getCalculationToTime(time);

        lastCalculationTimestamp = time;
        lastCalculation = total;
    }

    @Override
    public MealData getMealData() {
        MealData result = new MealData();

        for (Treatment treatment : treatments) {
            result.addTreatment(treatment);
        }
        return result;
    }

    @Override
    public List<Treatment> getTreatments() {
        return treatments;
    }

    @Subscribe
    public void onStatusEvent(final EventTreatmentChange ev) {
        initializeData();
        updateTotalIOB();
    }

}
