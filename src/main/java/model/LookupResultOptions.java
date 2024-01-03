package model;

import lombok.*;

@Data
@Builder
@Getter
@Setter
@ToString
public class LookupResultOptions {
    private LookupResult cheapestOption;
    private LookupResult cheapestAvailableOption;

}