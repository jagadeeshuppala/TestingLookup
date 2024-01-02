package model;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@Getter
@Setter
@ToString
public class LookupResult {
    private String description;
    private String available;
    private String priceString;
}