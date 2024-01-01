package model;

import lombok.*;

@Data
@Builder
@Getter
@Setter
@ToString
public class LookupResultOptions {
    private LookupResult chepestOption;
    private LookupResult chepestAvailableOption;


}