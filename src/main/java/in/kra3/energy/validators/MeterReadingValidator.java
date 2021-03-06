package in.kra3.energy.validators;

import in.kra3.energy.Utils;
import in.kra3.energy.models.MeterReading;
import in.kra3.energy.models.Profile;
import in.kra3.energy.repositories.MeterReadingRepository;
import in.kra3.energy.repositories.ProfileRepository;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Created by kra3 on 1/14/17.
 */
@Component
public class MeterReadingValidator implements Validator {
    private MeterReadingRepository meterReadingRepository;
    private ProfileRepository profileRepository;

    public MeterReadingValidator(MeterReadingRepository meterReadingRepository, ProfileRepository profileRepository) {
        this.meterReadingRepository = meterReadingRepository;
        this.profileRepository = profileRepository;
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return MeterReading.class.equals(aClass);
    }

    @Override
    public void validate(Object o, Errors errors) {
        MeterReading meterReading = (MeterReading) o;
        Utils utils = Utils.getInstance();

        String prevMonth = utils.previousMonth(meterReading.getMonth());
        Long lastMonthReading = 0L;

        if (prevMonth != "") {
            MeterReading byMonth = meterReadingRepository.findByMonth(prevMonth);
            lastMonthReading = byMonth.getReading();
        }

        if (lastMonthReading < meterReading.getReading()) {
            errors.reject("meter reading cannot be less than previous month's.");
        }

        Profile profile = profileRepository.findByProfileAndMonth(
                meterReading.getProfile(), meterReading.getMonth());

        MeterReading janReading = meterReadingRepository.findByProfileAndMonth(
                meterReading.getProfile(), "JAN");

        MeterReading decReading = meterReadingRepository.findByProfileAndMonth(
                meterReading.getProfile(), "DEC");

        if(profile != null && janReading != null && decReading != null){
            Long totalConsumption = decReading.getReading() - janReading.getReading();
            double allowed = totalConsumption * profile.getFraction();
            double tolerance = allowed * .25;

            long consumption = lastMonthReading - meterReading.getReading();
            if(consumption < allowed - tolerance && consumption > allowed + tolerance){
                errors.reject("Consumption is more than allowed/expected range.");
            }
        }
    }
}
