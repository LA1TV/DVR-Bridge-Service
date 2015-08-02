package uk.co.la1tv.dvrBridgeService.filters;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(1)
public class ApiSecretAuthFilter extends OncePerRequestFilter  {

	@Value("${auth.secret}")
	private String secret;
	
	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		
		String providedSecret = request.getParameter("secret");
		if (providedSecret == null || !providedSecret.equals(secret)) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid secret.");
			return;
		}
		
		filterChain.doFilter(request, response);
	}

}
