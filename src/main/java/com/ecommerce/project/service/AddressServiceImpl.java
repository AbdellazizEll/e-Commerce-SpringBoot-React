package com.ecommerce.project.service;


import com.ecommerce.project.config.util.AuthUtil;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Address;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.AddressDTO;
import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.repositories.AddressRepository;
import com.ecommerce.project.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressServiceImpl implements AddressService {


    @Autowired
    ModelMapper modelMapper;
    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public AddressDTO createAddress(AddressDTO addressDTO, User user) {

        Address address = modelMapper.map(addressDTO, Address.class);

        List<Address> addressList = user.getAdresses();
        addressList.add(address);
        user.setAdresses(addressList);
        address.setUser(user);
        Address savedAddress = addressRepository.save(address);

        return modelMapper.map(savedAddress, AddressDTO.class);


    }

    @Override
    public List<AddressDTO> getAddresses() {
        List<Address> addresses = addressRepository .findAll();

       List<AddressDTO>addressDTOS= addresses.stream()
                .map(address -> modelMapper.map(address, AddressDTO.class))
                .toList();

        return addressDTOS;
    }

    @Override
    public AddressDTO getAddressById(Long addressId) {

        Address address = addressRepository.findById(addressId).orElseThrow(
                () -> new ResourceNotFoundException("Address","addressId", addressId ));
    return modelMapper.map(address, AddressDTO.class);}

    @Override
    public List<AddressDTO> getUserAddresses(User user) {


        List<Address> addressList = user.getAdresses();

        return addressList.stream()
                .map(address ->  modelMapper.map(address, AddressDTO.class))
                .toList();
    }

    @Override
    public AddressDTO updateAddressById(Long addressId) {

        Address addressFromDatabase = addressRepository.findById(addressId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Address","addressId", addressId )
                );

        addressFromDatabase.setCountry(addressFromDatabase.getCountry());
        addressFromDatabase.setState(addressFromDatabase.getState());
        addressFromDatabase.setPincode(addressFromDatabase.getPincode());
        addressFromDatabase.setStreet(addressFromDatabase.getStreet());
        addressFromDatabase.setBuildingName(addressFromDatabase.getBuildingName());

        Address updatedAddress = addressRepository.save(addressFromDatabase);

        User user = addressFromDatabase.getUser();
        user.getAdresses().removeIf(address -> address.getAdress_id().equals(addressId));
        user.getAdresses().add(updatedAddress);

        userRepository.save(user);
        return modelMapper.map(updatedAddress, AddressDTO.class);

    }

    @Override
    public String deleteAddress(Long addressId) {
        Address address = addressRepository.findById(addressId).orElseThrow( () -> new ResourceNotFoundException("Address","AddressId",addressId));


    User user = address.getUser();
        user.getAdresses().removeIf(add -> add.getAdress_id().equals(addressId));
        userRepository.save(user);

        addressRepository.delete(address);
        return "Address deleted successfully with addressId " + addressId;    }


}


