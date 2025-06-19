import java.time.LocalDate;
import java.util.Objects;

public class DateRange {

    private LocalDate exactDate;
    private LocalDate dateFrom;
    private LocalDate dateTo;

    public DateRange(LocalDate exactDate, LocalDate dateFrom, LocalDate dateTo) {
        if (exactDate != null) {
            this.exactDate = exactDate;
            this.dateFrom = null;
            this.dateTo = null;
        } else if (dateFrom != null && dateTo != null) {
            this.exactDate = null;
            this.dateFrom = dateFrom;
            this.dateTo = dateTo;
        } else if (dateFrom == null && dateTo == null) {
            // Default to today as exactDate
            this.exactDate = LocalDate.now();
            this.dateFrom = null;
            this.dateTo = null;
        } else {
            throw new IllegalArgumentException("You must provide either both dateFrom and dateTo, or exactDate, or nothing.");
        }
    }

    public boolean isExactDate() {
        return exactDate != null;
    }

    public LocalDate getExactDate() {
        return exactDate;
    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    @Override
    public String toString() {
        if (isExactDate()) {
            return "Exact Date: " + exactDate;
        } else {
            return "Date Range: From " + dateFrom + " To " + dateTo;
        }
    }
}