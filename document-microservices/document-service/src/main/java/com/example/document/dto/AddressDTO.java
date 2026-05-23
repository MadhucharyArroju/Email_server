package com.example.document.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressDTO {


    private Long addressId;                              // ← Long

    private String state;                                // NOT unique

    private String city;                                 // NOT unique

    private String pincode;                              // NOT unique

    private String street;                               // NOT unique

}
