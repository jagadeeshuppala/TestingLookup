package model;

import lombok.*;

@Data
@Builder
@Getter
@Setter
@ToString
public class Product {
    private String productName;
    private String strength;
    private String packsize;
    private String productNameUnmodified;
    private String pOrPom;

    private int rowNumber;


}
