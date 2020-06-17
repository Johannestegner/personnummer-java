package dev.personnummer;

import javax.swing.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.format.DateTimeFormatter.ofPattern;

/**
 * Class used to validate Swedish personal identity numbers.
 *
 * @author Johannes Tegnér
 */
public final class Personnummer {
    private static final Pattern regexPattern;

    static {
        regexPattern = Pattern.compile("^(\\d{2})?(\\d{2})(\\d{2})(\\d{2})([-|+]?)?((?!000)\\d{3})(\\d?)$");
    }

    public static Personnummer parse(String personnummer, Options options) throws PersonnummerException {
        return new Personnummer(personnummer, options);
    }

    public static Personnummer parse(String personnummer) throws PersonnummerException {
        return parse(personnummer, new Options());
    }

    public static Personnummer parse(Long personnummer, Options options) throws PersonnummerException {
        return parse(Long.toString(personnummer), options);
    }

    public static Personnummer parse(Long personnummer) throws PersonnummerException {
        return parse(personnummer, new Options());
    }

    private final int realDay;
    private final String fullYear;
    private final String century;
    private final String year;
    private final String month;
    private final String day;
    private final String numbers;
    private final String controlNumber;
    private final boolean isMale;
    private final boolean isFemale;



    public Personnummer(String personnummer, Options options) throws PersonnummerException {
        if (personnummer == null) {
            throw new PersonnummerException();
        }

        Matcher matches = regexPattern.matcher(personnummer);
        if (!matches.find()) {
            throw new PersonnummerException();
        }

        String century;
        String decade = matches.group(2);
        if (matches.group(1) != null &&  !matches.group(1).isEmpty()) {
            century = matches.group(1);
        } else {
            //        LocalDate date = LocalDate.parse(ssn.longFormat.substring(0, ssn.longFormat.length() - 4), DateTimeFormatter.ofPattern("yyyyMMdd"));
            //        int years = (date.until(LocalDate.now())).getYears();
            int born = LocalDate.now().getYear() - Integer.parseInt(decade);
            if (!matches.group(5).isEmpty() && matches.group(5).equals("+")) {
                born -= 100;
            }

            century = Integer.toString(born).substring(0, 2);
        }

        int day = Integer.parseInt(matches.group(4));
        if (options.allowCoordinationNumber) {
            day = day > 60 ? day - 60 : day;
        } else if(day > 60) {
            throw new PersonnummerException();
        }

        this.realDay = day;
        this.century = century;
        this.year = decade;
        this.fullYear = century + decade;
        this.month = matches.group(3);
        this.day = matches.group(4);
        this.numbers = matches.group(6) + matches.group(7);
        this.controlNumber = matches.group(7);

        this.isMale =  Integer.parseInt(Character.toString(this.numbers.charAt(2))) % 2 == 1;
        this.isFemale = !this.isMale;

        // The format passed to Luhn method is supposed to be YYmmDDNNN
        // Hence all numbers that are less than 10 (or in last case 100) will have leading 0's added.
        if (luhn(String.format("%s%s%s%s", this.year, this.month, this.day, matches.group(6))) != Integer.parseInt(this.controlNumber)) {
            throw new PersonnummerException();
        }
    }

    public Personnummer(String personnummer) throws PersonnummerException {
        this(personnummer, new Options());
    }

    public Personnummer(Long personnummer, Options options) throws PersonnummerException {
        this(Long.toString(personnummer), options);
    }

    public Personnummer(Long personnummer) throws PersonnummerException {
        this(personnummer, new Options());
    }

    public int getAge() {
        return (LocalDate.of(Integer.parseInt(this.fullYear), Integer.parseInt(this.month), this.realDay).until(LocalDate.now())).getYears();
    }

    public String format() {
        return format(false);
    }

    public String format(boolean longFormat) {
        return (longFormat ? this.fullYear : this.year) + this.month + this.day + separator() + numbers;
    }

    public Boolean isMale() {
        return this.isMale;
    }

    public Boolean isFemale() {
        return this.isFemale;
    }

    public String separator() {
        return this.getAge() >= 100 ? "+" : "-";
    }

    public String getFullYear() {
        return fullYear;
    }

    public String getCentury() {
        return century;
    }

    public String getYear() {
        return year;
    }

    public String getMonth() {
        return month;
    }

    public String getDay() {
        return day;
    }

    public String getNumbers() {
        return numbers;
    }

    public String getControlNumber() {
        return controlNumber;
    }

    /**
     * Validate a Swedish personal identity number.
     *
     * @param personnummer personal identity number to validate, as string.
     * @return True if valid.
     */
    public static boolean valid(String personnummer) {
        try {
            parse(personnummer);
            return true;
        } catch (PersonnummerException ex) {
            return false;
        }
    }

    private static int luhn(String value) {
        // Luhn/mod10 algorithm. Used to calculate a checksum from the
        // passed value. The checksum is returned and tested against the control number
        // in the personal identity number to make sure that it is a valid number.

        int temp;
        int sum = 0;

        for (int i = 0; i < value.length(); i++) {
            temp = Character.getNumericValue(value.charAt(i));
            temp *= 2 - (i % 2);
            if (temp > 9)
                temp -= 9;

            sum += temp;
        }

        return (int)(Math.ceil((double)sum / 10.0) * 10.0 - (double)sum);
    }

    private static boolean testDate(int year, int month, int day) {
        try {
            DateFormat df = new SimpleDateFormat("yy-MM-dd");
            df.setLenient(false);
            df.parse(String.format("%02d-%02d-%02d", year, month, day));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

}
