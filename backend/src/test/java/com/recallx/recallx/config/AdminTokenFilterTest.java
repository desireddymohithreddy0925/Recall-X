package com.recallx.recallx.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AdminTokenFilterTest {

    @Test
    void anAdminCallWithoutTheTokenIsRefused() throws Exception {
        MockHttpServletResponse response = run(new AdminTokenFilter("secret"), "/api/admin/memory/sync", null);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("forbidden");
    }

    @Test
    void anAdminCallWithTheWrongTokenIsRefused() throws Exception {
        assertThat(run(new AdminTokenFilter("secret"), "/api/admin/demo/reset", "guess").getStatus()).isEqualTo(403);
    }

    @Test
    void anAdminCallWithTheTokenGoesThrough() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletRequest request = request("/api/admin/demo/reset", "secret");
        new AdminTokenFilter("secret").doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void withNoTokenConfiguredEveryAdminCallIsRefused() throws Exception {
        MockHttpServletResponse response = run(new AdminTokenFilter(""), "/api/admin/memory/sync", "");

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("RECALLX_ADMIN_TOKEN");
    }

    @Test
    void otherEndpointsNeedNoToken() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        new AdminTokenFilter("secret").doFilter(request("/api/ask", null), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    private static MockHttpServletResponse run(AdminTokenFilter filter, String uri, String token) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request(uri, token), response, new MockFilterChain());
        return response;
    }

    private static MockHttpServletRequest request(String uri, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        if (token != null) request.addHeader(AdminTokenFilter.HEADER, token);
        return request;
    }
}
