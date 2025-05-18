package org.example.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            model.addAttribute("status", statusCode);
            model.addAttribute("error", message != null ? message : getErrorMessage(statusCode));
        } else {
            model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            model.addAttribute("error", "Неизвестная ошибка");
        }

        return "error";
    }

    private String getErrorMessage(int statusCode) {
        if (statusCode == HttpStatus.NOT_FOUND.value()) {
            return "Страница не найдена";
        } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            return "Внутренняя ошибка сервера";
        } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
            return "Доступ запрещён";
        } else {
            return "Произошла ошибка";
        }
    }
}