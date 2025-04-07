package ru.kata.spring.boot_security.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.service.RoleService;
import ru.kata.spring.boot_security.demo.service.UserService;

import javax.validation.Valid;
import java.security.Principal;
import java.util.Set;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final RoleService roleService;

    public AdminController(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    @GetMapping
    public String adminPanel(Model model, Principal principal) {
        User currentUser = userService.findByUsername(principal.getName());

        model.addAttribute("user", currentUser); // текущий пользователь
        model.addAttribute("users", userService.getAllUsers()); // все пользователи
        model.addAttribute("allRoles", roleService.getAllRoles()); // все роли

        return "admin";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("allRoles", roleService.getAllRoles());
        return "new";
    }

    @PostMapping("/new")
    public String createUser(@Valid @ModelAttribute("user") User user,
                             BindingResult bindingResult,
                             @RequestParam(required = false) Set<Long> roleIds,
                             Model model) {

        // Проверка ролей
        if (roleIds == null || roleIds.isEmpty()) {
            model.addAttribute("user", user); // сохраняем введенные данные
            model.addAttribute("allRoles", roleService.getAllRoles());
            model.addAttribute("roleError", "Выберите минимум 1 роль");
            return "new";
        }

        // Обработка ошибок валидации
        if (bindingResult.hasErrors()) {
            model.addAttribute("allRoles", roleService.getAllRoles());
            model.addAttribute("user", user); // сохраняем введенные данные
            return "new";
        }

        // Проверка существующего email
        if (userService.existsByEmail(user.getEmail())) {
            model.addAttribute("user", user); // сохраняем введенные данные
            model.addAttribute("allRoles", roleService.getAllRoles());
            model.addAttribute("error", "Email уже зарегистрирован");
            return "new";
        }

        userService.saveUser(user, roleIds);
        return "redirect:/admin";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        model.addAttribute("allRoles", roleService.getAllRoles());
        return "edit";
    }

    @PostMapping("/edit/{id}")
    public String updateUser(@PathVariable Long id,
                             @Valid @ModelAttribute("user") User user,
                             BindingResult bindingResult,
                             @RequestParam(required = false) Set<Long> roleIds,
                             Model model) {

        if (roleIds == null || roleIds.isEmpty()) {
            model.addAttribute("roleError", "Выберите хотя бы одну роль");
            model.addAttribute("allRoles", roleService.getAllRoles());
            return "edit";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("allRoles", roleService.getAllRoles());
            return "edit";
        }
        User existingUser = userService.getUserById(id);

        if (!existingUser.getEmail().equals(user.getEmail())
                && userService.existsByEmail(user.getEmail())) {
            model.addAttribute("error", "Email уже зарегистрирован");
            model.addAttribute("allRoles", roleService.getAllRoles());
            return "edit";
        }

        userService.updateUser(id, user, roleIds);
        return "redirect:/admin";
    }

    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/admin";
    }
}
