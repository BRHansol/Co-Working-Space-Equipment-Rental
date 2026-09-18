package com.example.roombooking.service.impl;
import com.example.roombooking.common.PageResponse;
import  com.example.roombooking.domain.entity.User;
import com.example.roombooking.repository.UserRepository;
import com.example.roombooking.service.UserService;


// รอเพิ่ม transactional annotation 
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override 
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override 
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override 
    public User createUser(User user) {
        return userRepository.save(user);
    }

    @Override 
    public PageResponse<User> getAllUsers(int page, int size) {
        return null;
    }

    @Override 
    public boolean exitsById(Long id) {
        return userRepository.existsById(id);
    }

    @Override 
    public void deleteUserById(Long id) {
        userRepository.deleteById(id);
    }
}
