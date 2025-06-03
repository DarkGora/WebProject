package org.example.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
public class CustomErrorController implements ErrorController {

    private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        // Получение кода состояния
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        // Получение запрошенного URL
        String requestUri = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
        requestUri = Optional.ofNullable(requestUri).orElse("Неизвестный URL");

        // Инициализация кода состояния и сообщения
        int statusCode = Optional.ofNullable(status)
                .map(Object::toString)
                .map(Integer::parseInt)
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR.value());
        String errorMessage = Optional.ofNullable(message)
                .map(Object::toString)
                .filter(msg -> !msg.isEmpty())
                .orElse(getErrorMessage(statusCode));

        // Логирование ошибки
        String exceptionMessage = Optional.ofNullable(exception)
                .map(Object::toString)
                .orElse("Нет дополнительной информации");
        logger.error("Ошибка: код={}, сообщение={}, URL={}, исключение={}",
                statusCode, errorMessage, requestUri, exceptionMessage);

        // Передача данных в модель
        model.addAttribute("status", statusCode);
        model.addAttribute("error", errorMessage);
        model.addAttribute("requestUri", requestUri);

        return "error";
    }

    private String getErrorMessage(int statusCode) {
        return switch (statusCode) {
            case 400 -> "Некорректный запрос";
            case 401 -> "Неавторизованный доступ";
            case 403 -> "Доступ запрещён";
            case 404 -> "Страница не найдена";
            case 429 -> "Слишком много запросов";
            case 500 -> "Внутренняя ошибка сервера";
            case 503 -> "Сервис временно недоступен";
            default -> "Произошла неизвестная ошибка";
        };
    }
}