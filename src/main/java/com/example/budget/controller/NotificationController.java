package com.example.budget.controller;

import com.example.budget.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("activeNav", "notifications");
        model.addAttribute("notifications", notificationService.recent());
        model.addAttribute("unreadCount", notificationService.unreadCount());
        return "notifications";
    }

    @PostMapping("/{id}/read")
    public String markRead(@PathVariable long id, RedirectAttributes redirect) {
        notificationService.markRead(id);
        redirect.addFlashAttribute("message", "通知を既読にしました");
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String markAllRead(RedirectAttributes redirect) {
        notificationService.markAllRead();
        redirect.addFlashAttribute("message", "すべての通知を既読にしました");
        return "redirect:/notifications";
    }
}
